package net.imglib2.test.cache.img;

import static net.imglib2.cache.img.N5CachedCellImgOptions.options;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.cache.img.CellLoader;
import net.imglib2.cache.img.N5CachedCellImgFactory;
import net.imglib2.cache.img.N5CachedCellImgOptions;
import net.imglib2.cache.img.ReadOnlyCachedCellImgOptions.CacheType;
import net.imglib2.cache.img.SingleCellArrayImg;
import net.imglib2.img.cell.CellCursor;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.img.cell.CellLocalizingCursor;
import net.imglib2.img.cell.CellRandomAccess;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.Util;

/**
 * Integration test checking whether a N5-backed cache that saves blocks to a known folder can be opened as another cache.
 * If the values of the reloaded cache match the values in the original cache, N5 blocks were written and read
 * correctly.
 *  
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 */
public class N5CacheTest {
	@Test
	public void testCache() throws IOException, InterruptedException {
		final int[] cellDimensions = new int[] { 64, 64, 64 };
		// FIXME: on my machine, smaller test volumes somehow cause the GuardedStrongRefLoaderRemoverCache 
		// not to write any blocks at all (onRemove is never called / no items get ever queued for removal).
		// What is going on there?
		final long[] dimensions = new long[] { 640, 640, 128 };

		final Path cacheDir = Files.createTempDirectory("cache");

		final N5CachedCellImgOptions options = options()
				.cellDimensions( cellDimensions )
				.dirtyAccesses(false)
				.volatileAccesses(false)
				.cacheType( CacheType.BOUNDED )
				.maxCacheSize( 0 ) // this forces the cache to evict the cells immediately, hence they will all be written to disk
				.cacheDirectory(cacheDir)
				.deleteCacheDirectoryOnExit(false);

		final CellGrid cellGrid = new CellGrid( dimensions, cellDimensions ) ;
		
		final CellLoader< UnsignedByteType > loader = new CheckerboardLoader( cellGrid );
		final CachedCellImg< UnsignedByteType, ? > img = new N5CachedCellImgFactory<>( new UnsignedByteType(), options ).create(
				dimensions,
				loader );

		// touch all data multiple times to make sure it gets cached and blocks get dropped
		final CellCursor<UnsignedByteType, ?> cacheCursor = img.cursor();
		for(int i = 0; i < 2; i++) {
			cacheCursor.reset();
			while(cacheCursor.hasNext()) {
				cacheCursor.next();
			}
		}
		
		final N5CachedCellImgOptions reloadOptions = options()
				.cellDimensions( cellDimensions )
				.cacheType( CacheType.BOUNDED )
				.cacheDirectory(cacheDir);
		final CachedCellImg< UnsignedByteType, ? > reloadedImg = new N5CachedCellImgFactory<>( new UnsignedByteType(), reloadOptions ).create(
				dimensions, new CellLoader<UnsignedByteType>() {
					@Override
					public void load(SingleCellArrayImg<UnsignedByteType, ?> cell) throws Exception {
						fail("Cell " + Util.printInterval(cell) + " should be loaded from disk, not from cell loader!");
					}
				} );

		CellLocalizingCursor<UnsignedByteType, ?> imgCursor = img.localizingCursor();
		CellRandomAccess<UnsignedByteType, ?> reloadedImgAccess = reloadedImg.randomAccess();

		while(imgCursor.hasNext()) {
			imgCursor.fwd();
			reloadedImgAccess.setPosition(imgCursor);
			assertEquals("Values did not match at location " + Util.printCoordinates(imgCursor), imgCursor.get(), reloadedImgAccess.get());
		}
	}
}
