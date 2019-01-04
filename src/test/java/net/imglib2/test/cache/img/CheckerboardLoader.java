package net.imglib2.test.cache.img;

import net.imglib2.cache.img.CellLoader;
import net.imglib2.cache.img.SingleCellArrayImg;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.util.Util;

public class CheckerboardLoader implements CellLoader< UnsignedByteType >
{
	private final CellGrid grid;

	public CheckerboardLoader( final CellGrid grid )
	{
		this.grid = grid;
	}

	@Override
	public void load( final SingleCellArrayImg< UnsignedByteType, ? > cell ) throws Exception
	{
		System.out.println("Loading cell " + Util.printInterval(cell));
		final int n = grid.numDimensions();
		long sum = 0;
		for ( int d = 0; d < n; ++d )
			sum += cell.min( d ) / grid.cellDimension( d );
		final int color = ( sum % 2 == 0 ) ? 1 : 0;
		cell.forEach( t -> t.set( color ) );
	}
}
