package net.imglib2.cache.img;

import java.io.IOException;
import java.nio.file.Path;

import org.janelia.saalfeldlab.n5.Compression;

import net.imglib2.Dirty;
import net.imglib2.cache.CacheLoader;
import net.imglib2.cache.CacheRemover;
import net.imglib2.cache.IoSync;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.type.NativeType;
import net.imglib2.util.Fraction;

/**
 * {@link N5CellCache} that can handle dirty cells
 *
 * @param <A> access type
 * @author Tobias Pietzsch
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @author Gabriel Einsdorf, KNIME GmbH, Konstanz, Germany
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 */
public class DirtyN5CellCache<T extends NativeType<T>, A extends Dirty & ArrayDataAccess<A>>
        extends N5CellCache<T, A> {
        public DirtyN5CellCache(
                final Path resultCacheLocation, 
                final String datasetName,
                final CellGrid grid,
                final CacheLoader<Long, Cell<A>> backingLoader, 
                final Fraction entitiesPerPixel, 
                final AccessIo<A> accessIo,
                final Compression compression, 
                final T type) throws IOException
        {
            super( resultCacheLocation, datasetName, grid, backingLoader, entitiesPerPixel, accessIo, compression, type );
        }
    
        @Override
        public void onRemoval( final Long key, final Cell< A > value )
        {
            if ( value.getData().isDirty() )
                super.onRemoval( key, value );
        }
}
