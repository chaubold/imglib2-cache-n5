package net.imglib2.cache.img;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.GzipCompression;
import org.janelia.saalfeldlab.n5.N5FSReader;
import org.janelia.saalfeldlab.n5.N5FSWriter;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.janelia.saalfeldlab.n5.imglib2.N5CellLoader;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;

import net.imglib2.Dirty;
import net.imglib2.cache.CacheLoader;
import net.imglib2.cache.CacheRemover;
import net.imglib2.cache.IoSync;
import net.imglib2.img.basictypeaccess.ArrayDataAccessFactory;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.type.NativeType;
import net.imglib2.type.NativeTypeFactory;
import net.imglib2.util.Fraction;
import net.imglib2.util.IntervalIndexer;
import net.imglib2.util.Intervals;

/**
 * Basic {@link CacheRemover}/{@link CacheLoader} for writing/reading cells to a
 * n5 Image.
 * <p>
 * Blocks which are not in the output n5 cache (yet) are obtained from a backing
 * {@link CacheLoader}. The availability of N5 blocks is managed by a HashSet m_cachedSet, 
 * because N5 currently does not provide a method to check whether a block exists,
 * it simply returns zeros if that's not the case.
 * </p>
 * <p>
 * <em> A {@link N5CellCache} should be connected to a in-memory cache through
 * {@link IoSync} if the cache will be used concurrently by multiple threads!
 * </em>
 * </p>
 *
 * @param <A> access type
 * @author Tobias Pietzsch
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @author Gabriel Einsdorf, KNIME GmbH, Konstanz, Germany
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 */
public class N5CellCache<T extends NativeType<T>, A extends ArrayDataAccess<A>>
        implements CacheRemover<Long, Cell<A>>, CacheLoader<Long, Cell<A>> {

    // the I/O componentes
    private final N5Reader m_reader;

    private final N5CellLoader<T> m_loader;

    private final N5Writer m_writer;

    private final CacheLoader<Long, Cell<A>> m_backingLoader;

    private final N5CellStorer<A> m_storer;

    private final Set<Long> m_cachedSet = new HashSet<>();

    private final CellGrid m_grid;

    private final Fraction m_entitiesPerPixel;

    private final ArrayDataAccess<A> m_creator;

    private final String m_dataset;

    private final DatasetAttributes m_attributes;

    private T m_type;

    /**
     * @param cellgrid
     * @param backingLoader
     * @param entitiesPerPixel
     * @param accessIo
     * @throws IOException
     */

    public N5CellCache(final CellGrid cellgrid, final CacheLoader<Long, Cell<A>> backingLoader,
            final Fraction entitiesPerPixel, final AccessIo<A> accessIo, final T type) throws IOException {
        this(Files.createTempDirectory("n5cellCache"), "cache", cellgrid, backingLoader, entitiesPerPixel, accessIo,
                new GzipCompression(), type);
    }

    /**
     * Creates a {@link N5CellCache} with the given parameters, the cache will cache
     * results to the given location. If the N5 file and dataset already exist with matching block size,
     * the cache will use the blocks that are already stored there.
     *
     * @param resultCacheLocation the path to the location where this cache will
     *                            store its cells
     * @param datasetName         the name of the n5 dataset to use
     * @param grid                the cell grid of the tensor
     * @param backingLoader       the loader backing this cache
     * @param entitiesPerPixel
     * @param accessIo
     * @param compression         the {@link Compression} to use
     * @param type
     * @throws IOException
     */
    public N5CellCache(
            final Path resultCacheLocation, 
            final String datasetName,
            final CellGrid grid,
            final CacheLoader<Long, Cell<A>> backingLoader, 
            final Fraction entitiesPerPixel, 
            final AccessIo<A> accessIo,
            final Compression compression, 
            final T type) throws IOException {
        m_grid = grid;
        m_backingLoader = backingLoader;
        m_entitiesPerPixel = entitiesPerPixel;
        m_type = type;

        final String datasetLoc = resultCacheLocation.toAbsolutePath().toString();
        m_dataset = datasetName;

        // extract image dimensions
        final long[] imgDims = m_grid.getImgDimensions();
        final int[] cellDims = new int[m_grid.numDimensions()];
        m_grid.cellDimensions(cellDims);

        final DataType dataType = N5Utils.dataType(type);
        m_creator = ArrayDataAccessFactory.get(type);

        // Init the reader & writer
        final N5FSWriter fswriter = new N5FSWriter(datasetLoc);
        if (fswriter.datasetExists(m_dataset)) {
            // check that dataset properties match!
            final DatasetAttributes datasetAttribs = fswriter.getDatasetAttributes(m_dataset);
            if (datasetAttribs.getDataType() != dataType) {
                throw new IOException("Cache dataset exists already, but data types don't match");
            }
            if (datasetAttribs.getNumDimensions() != m_grid.numDimensions()) {
                throw new IOException("Cache dataset exists already, but num dimensions doesn't match");
            }
            if (!Arrays.equals(datasetAttribs.getDimensions(), imgDims)) {
                throw new IOException("Cache dataset exists already, but image size doesn't match");
            }
            if (!Arrays.equals(datasetAttribs.getBlockSize(), cellDims)) {
                throw new IOException("Cache dataset exists already, but block size doesn't match");
            }

            N5CellCache.forEachPresentBlockIdx(datasetLoc, m_dataset, m_grid, m_cachedSet::add);
        } else {
            fswriter.createDataset(m_dataset, imgDims, cellDims, dataType, compression);
        }
        m_writer = fswriter;
        m_reader = new N5FSReader(datasetLoc);

        m_attributes = m_writer.getDatasetAttributes(m_dataset);
        final int[] blockSize = m_attributes.getBlockSize();

        // init the IO
        m_loader = new N5CellLoader<>(m_reader, m_dataset, blockSize);
        m_storer = new N5CellStorer<>(m_writer, m_dataset, cellDims, accessIo, m_entitiesPerPixel);
    }

    protected static void forEachPresentBlockIdx(final String filename, final String dataset, final CellGrid grid,
            final Consumer<Long> func) throws IOException {
        Files.walkFileTree(Paths.get(filename, dataset), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                final String pattern = Pattern.quote(System.getProperty("file.separator"));
                final String[] cellPath = file.toString().split(pattern);

                if (cellPath[cellPath.length - 1].equals("attributes.json")) {
                    // skip the attributes.json file
                    return FileVisitResult.CONTINUE;
                }

                final int n = grid.numDimensions();
                final long[] pos = new long[n];
                for (int d = 0; d < n; d++) {
                    pos[d] = Long.valueOf(cellPath[cellPath.length - n + d]);
                }
                func.accept(IntervalIndexer.positionToIndex(pos, grid.getGridDimensions()));
                return FileVisitResult.CONTINUE;
            }
        });
    }

    @Override
    public Cell<A> get(final Long key) throws Exception {
        // Look at n5 reader
        final long index = key;

        // cell is already cached
        if (m_cachedSet.contains(key)) {
            final long[] cellMin = new long[m_grid.numDimensions()];
            final int[] cellDims = new int[m_grid.numDimensions()];
            m_grid.getCellDimensions(index, cellMin, cellDims);

            final long numEntities = m_entitiesPerPixel.mulCeil(Intervals.numElements(cellDims));
            final A array = m_creator.createArray((int) numEntities);
            
            @SuppressWarnings( { "rawtypes", "unchecked" } )
            final SingleCellArrayImg<T, A> img = new SingleCellArrayImg(cellDims, cellMin, array, new AlwaysClean());
            @SuppressWarnings( "unchecked" )
            final NativeTypeFactory<T, A> info = (NativeTypeFactory<T, A>) m_type.getNativeTypeFactory();
            img.setLinkedType(info.createLinkedType(img));
            m_loader.load(img);
            return new Cell<>(cellDims, cellMin, array);
        }
        // fall back on backing loader
        return m_backingLoader.get(key);
    }

    @Override
    public void onRemoval(final Long key, final Cell<A> value) {

        if (!m_cachedSet.contains(key)) {
            try {
                m_storer.store(value);
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
            m_cachedSet.add(key);
        } else {
            // ignore, have we already persisted this cell.
        }
    }

    private class AlwaysClean implements Dirty {

        @Override
        public boolean isDirty() {
            return false;
        }

        @Override
        public void setDirty() {
            // NO-Op
        }
    }
}
