/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 */
package net.imglib2.cache.img;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.janelia.saalfeldlab.n5.DataBlock;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Writer;

import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.cell.Cell;
import net.imglib2.util.Fraction;
import net.imglib2.util.Intervals;

/**
 * A {@link CellStorer} backed by N5.
 *
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @author Gabriel Einsdorf, KNIME GmbH, Konstanz, Germany
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 */
public class N5CellStorer<A extends ArrayDataAccess<A>> implements CellStorer<A> {

    private final N5Writer m_n5;

    private final String m_dataSet;

    private final int[] m_cellDimensions;

    private final DatasetAttributes m_datasetAttributes;

    private final AccessIo<A> m_accessIo;

    private final Fraction m_entitiesPerPixel;

    /**
     * @param n5
     * @param dataSet
     * @param cellDimensions
     * @param accessIo
     * @param entitiesPerPixel
     * @throws IOException
     */
    public N5CellStorer(final N5Writer n5, final String dataSet, final int[] cellDimensions, final AccessIo<A> accessIo,
        final Fraction entitiesPerPixel) throws IOException {
        m_n5 = n5;
        m_dataSet = dataSet;
        m_cellDimensions = cellDimensions;
        m_accessIo = accessIo;
        m_entitiesPerPixel = entitiesPerPixel;
        m_datasetAttributes = n5.getDatasetAttributes(dataSet);
    }

    @Override
    public void store(final Cell<A> cell) throws IOException {
        m_n5.writeBlock(m_dataSet, m_datasetAttributes, new CellDataBlock<>(cell));
    }

    /**
     * Custom {@link DataBlock} implementation that wraps Imglib2Cells.
     * 
     * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
     * @author Gabriel Einsdorf, KNIME GmbH, Konstanz, Germany
     * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
     * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
     */
    private class CellDataBlock<T> implements DataBlock<T> {

        private final Cell<A> m_cell;

        private final int[] m_localCellDims;

        private final long[] m_gridpos;

        public CellDataBlock(final Cell<A> cell) {
            final long[] gridPosition = new long[m_cellDimensions.length];
            for (int d = 0; d < gridPosition.length; ++d) {
                gridPosition[d] = cell.min(d) / m_cellDimensions[d];
            }

            m_cell = cell;

            // cells at the border of the tensor can be partial, so we need to check the actual size.
            m_localCellDims = m_cellDimensions.clone();
            m_cell.dimensions(m_localCellDims);
            m_gridpos = gridPosition;
        }

        @Override
        public int[] getSize() {
            return m_localCellDims;
        }

        @Override
        public long[] getGridPosition() {
            return m_gridpos;
        }

        @Override
        public T getData() {
        	// TODO: use cell's data?
            throw new UnsupportedOperationException("This block's data can not be accessed directly!");
        }

        @Override
        public ByteBuffer toByteBuffer() {
            final long blocksize = m_entitiesPerPixel.mulCeil(Intervals.numElements(m_localCellDims));
            final long bytesize = blocksize * m_accessIo.getBytesPerElement();

            final ByteBuffer out = ByteBuffer.allocate((int)bytesize);
            m_accessIo.save(m_cell.getData(), out, (int)blocksize);
            return out;
        }

        @Override
        public void readData(final ByteBuffer buffer) {
            throw new UnsupportedOperationException("This block is read only!");
        }

        @Override
        public int getNumElements() {
            return (int)Intervals.numElements(m_localCellDims);
        }
    }
}
