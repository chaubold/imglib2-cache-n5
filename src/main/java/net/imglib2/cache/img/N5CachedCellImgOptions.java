/*
 * #%L
 * BigtaViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2015 BigDataViewer authors
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package net.imglib2.cache.img;

import java.nio.file.Path;

import org.janelia.saalfeldlab.n5.Compression;
import org.janelia.saalfeldlab.n5.GzipCompression;

import net.imglib2.cache.img.ReadOnlyCachedCellImgOptions.CacheType;
import net.imglib2.img.cell.CellImgFactory;

/**
 * Optional parameters for constructing a {@link N5CachedCellImgFactory}.
 *
 * @author Tobias Pietzsch
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @author Gabriel Einsdorf, KNIME GmbH, Konstanz, Germany
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 */
public class N5CachedCellImgOptions extends AbstractReadWriteCachedCellImgOptions {
    public final Values values;

    N5CachedCellImgOptions(final Values values) {
        this.values = values;
    }

    @Override
	Values values() {
		return values;
	}

	@Override
	public N5CachedCellImgOptions merge(final AbstractReadWriteCachedCellImgOptions other) {
		return new N5CachedCellImgOptions(new Values(values, other.values()));
	}

    public N5CachedCellImgOptions() {
        this(new Values());
    }

    /**
     * Create default {@link N5CachedCellImgOptions}.
     *
     * @return default {@link N5CachedCellImgOptions}.
     */
    public static N5CachedCellImgOptions options() {
        return new N5CachedCellImgOptions();
    }

    @Override
	public AbstractReadWriteCachedCellImgOptions dirtyAccesses(boolean dirty) {
		return new N5CachedCellImgOptions(values.copy().setDirtyAccesses(dirty));
	}

	@Override
	public AbstractReadWriteCachedCellImgOptions volatileAccesses(boolean volatil) {
		return new N5CachedCellImgOptions(values.copy().setVolatileAccesses(volatil));
	}

    @Override
    public N5CachedCellImgOptions numIoThreads(final int numIoThreads) {
        return new N5CachedCellImgOptions(values.copy().setNumIoThreads(numIoThreads));
    }

    @Override
    public N5CachedCellImgOptions maxIoQueueSize(final int maxIoQueueSize) {
        return new N5CachedCellImgOptions(values.copy().setMaxIoQueueSize(maxIoQueueSize));
    }

    @Override
    public N5CachedCellImgOptions cacheType(final CacheType cacheType) {
        return new N5CachedCellImgOptions(values.copy().setCacheType(cacheType));
    }

    @Override
    public N5CachedCellImgOptions maxCacheSize(final long maxCacheSize) {
        return new N5CachedCellImgOptions(values.copy().setMaxCacheSize(maxCacheSize));
    }

    /**
     * Specify a loaction where the cache should read and write the cached blocks. If the path already exists, it is
     * assumed to be a valid N5 file with the same configuration as this cache, providing access to previously cached
     * blocks.
     *
     * @param cacheDir The path at which the cache will save blocks to N5
     */
    public N5CachedCellImgOptions cacheDir(final Path cacheDir) {
        return new N5CachedCellImgOptions(values.copy().setCacheDir(cacheDir));
    }

    /**
     * Set the dimensions of a cell. This is extended or truncated as necessary. For example if
     * {@code cellDimensions=[64,32]} then for creating a 3D image it will be augmented to {@code [64,32,32]}. For
     * creating a 1D image it will be truncated to {@code [64]}.
     *
     * @param cellDimensions dimensions of a cell (default is 10).
     */
    @Override
    public N5CachedCellImgOptions cellDimensions(final int... cellDimensions) {
        CellImgFactory.verifyDimensions(cellDimensions);
        return new N5CachedCellImgOptions(values.copy().setCellDimensions(cellDimensions));
    }

    /**
     * Specify whether cells initialized by a {@link CellLoader} should be marked as dirty. It is useful to set this to
     * {@code true} if initialization is a costly operation. By this, it is made sure that cells are initialized only
     * once, and then written and retrieve from the disk cache when they are next required.
     * <p>
     * This is {@code false} by default.
     * </p>
     * <p>
     * This option only has an effect for {@link N5CachedCellImg} that are created with a {@link CellLoader}
     * ({@link DiskCachedCellImgFactory#create(long[], net.imglib2.type.NativeType, CellLoader)}).
     * </p>
     *
     * @param initializeAsDirty whether cells initialized by a {@link CellLoader} should be marked as dirty.
     */
    public N5CachedCellImgOptions initializeCellsAsDirty(final boolean initializeAsDirty) {
        return new N5CachedCellImgOptions(values.copy().setInitializeCellsAsDirty(initializeAsDirty));
    }

    public N5CachedCellImgOptions compression(final Compression compression) {
        return new N5CachedCellImgOptions(values.copy().setCompression(compression));
    }

    public N5CachedCellImgOptions datasetName(final String datasetName) {
        return new N5CachedCellImgOptions(values.copy().setDatasetName(datasetName));
    }

    /**
     * Read-only {@link N5CachedCellImgOptions} values.
     */
    public static class Values extends AbstractReadWriteCachedCellImgOptions.Values {
        /**
         * Copy constructor.
         */
        Values(final Values that) {
            super(that);
            this.cacheDir = that.cacheDir;
            this.datasetName = that.datasetName;
            this.compression = that.compression;
        }

        Values() {
        }

        Values(final Values base, final Values aug) {
            super(base, aug);
            cacheDir = aug.cacheDirModified ? aug.cacheDir : base.cacheDir;
            datasetName = aug.datasetNameModified ? aug.datasetName : base.datasetName;
            compression = aug.compressionModified ? aug.compression : base.compression;
        }

        Values( final Values base, final AbstractReadWriteCachedCellImgOptions.Values aug )
		{
			super(base, aug);
            cacheDir = base.cacheDir;
            datasetName = base.datasetName;
            compression = base.compression;
		}

        public N5CachedCellImgOptions optionsFromValues() {
            return new N5CachedCellImgOptions(new Values(this));
        }

        private Path cacheDir = null;

        private String datasetName = "cache";

        private Compression compression = new GzipCompression();

        public Path cacheDir() {
            return cacheDir;
        }

        public String datasetName() {
            return datasetName;
        }

        public Compression compression() {
            return compression;
        }

        private boolean cacheDirModified = false;

        private boolean datasetNameModified = false;

        private boolean compressionModified = false;

        @Override
        Values setDirtyAccesses(final boolean b) {
            super.setDirtyAccesses(b);
            return this;
        }

        @Override
		Values setVolatileAccesses( final boolean b )
		{
			super.setVolatileAccesses(b);
			return this;
		}

        @Override
        Values setNumIoThreads(final int n) {
            super.setNumIoThreads(n);
            return this;
        }

        @Override
        Values setMaxIoQueueSize(final int n) {
            super.setMaxIoQueueSize(n);
            return this;
        }

        @Override
        Values setCacheType(final CacheType t) {
            super.setCacheType(t);
            return this;
        }

        @Override
        Values setMaxCacheSize(final long n) {
            super.setMaxCacheSize(n);
            return this;
        }

        @Override
        Values setCellDimensions(final int[] dims) {
            super.setCellDimensions(dims);
            return this;
        }

        @Override
        Values setInitializeCellsAsDirty(final boolean initializeAsDirty) {
            super.setInitializeCellsAsDirty(initializeAsDirty);
            return this;
        }

        Values setCacheDir(final Path cacheDir) {
            this.cacheDir = cacheDir;
            cacheDirModified = true;
            return this;
        }

        Values setDatasetName(final String datasetName) {
            this.datasetName = datasetName;
            datasetNameModified = true;
            return this;
        }

        Values setCompression(final Compression compression) {
            this.compression = compression;
            compressionModified = true;
            return this;
        }

        Values copy() {
            return new Values(this);
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append(super.toString());
            sb.append("N5CachedCellImgOptions = {");

            sb.append("cacheDir = ");
            if (null != cacheDir) {
                sb.append(cacheDir.toAbsolutePath().toString());
            } else {
                sb.append("null");
            }
            if (cacheDirModified)
                sb.append( " [m]" );
            sb.append(", ");

            sb.append("datasetName = ");
            sb.append(datasetName);
            if (datasetNameModified)
                sb.append( " [m]" );
            sb.append(", ");

            sb.append("compression = ");
            if (null != cacheDir) {
                sb.append(compression.getType());
            } else {
                sb.append("null");
            }
            if (compressionModified)
                sb.append( " [m]" );
            sb.append(", ");

            sb.append("}");

            return sb.toString();
        }
    }
}
