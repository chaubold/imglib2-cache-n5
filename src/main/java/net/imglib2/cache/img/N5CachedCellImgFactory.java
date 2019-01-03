/*
 * #%L
 * ImgLib2: a general-purpose, multidimensional image processing library.
 * %%
 * Copyright (C) 2009 - 2016 Tobias Pietzsch, Stephan Preibisch, Stephan Saalfeld,
 * John Bogovic, Albert Cardona, Barry DeZonia, Christian Dietz, Jan Funke,
 * Aivar Grislis, Jonathan Hale, Grant Harris, Stefan Helfrich, Mark Hiner,
 * Martin Horn, Steffen Jaensch, Lee Kamentsky, Larry Lindsey, Melissa Linkert,
 * Mark Longair, Brian Northan, Nick Perry, Curtis Rueden, Johannes Schindelin,
 * Jean-Yves Tinevez and Michael Zinsmaier.
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import net.imglib2.Dimensions;
import net.imglib2.cache.Cache;
import net.imglib2.cache.CacheLoader;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.type.NativeType;
import net.imglib2.type.NativeTypeFactory;
import net.imglib2.util.Fraction;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;

/**
 * Factory for creating {@link N5CachedCellImg}s. See
 * {@link N5CachedCellImgOptions} for available configuration options and
 * defaults.
 *
 * @author Tobias Pietzsch
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @author Gabriel Einsdorf, KNIME GmbH, Konstanz, Germany
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 */
public class N5CachedCellImgFactory<T extends NativeType<T>> extends AbstractReadWriteCachedCellImgFactory<T> {
    private N5CachedCellImgOptions factoryOptions;

    /**
     * Create a new {@link N5CachedCellImgFactory} with default configuration.
     */
    public N5CachedCellImgFactory(final T type) {
        this(type, N5CachedCellImgOptions.options());
    }

    /**
     * Create a new {@link N5CachedCellImgFactory} with the specified configuration.
     *
     * @param optional configuration options.
     */
    public N5CachedCellImgFactory(final T type, final N5CachedCellImgOptions optional) {
        super(type);
        this.factoryOptions = optional;
    }

    @Override
    public N5CachedCellImg<T, ?> create(final long... dimensions) {
        return create(dimensions, null, null, type(), null);
    }

    @Override
    public N5CachedCellImg<T, ?> create(final Dimensions dimensions) {
        return create(Intervals.dimensionsAsLongArray(dimensions));
    }

    @Override
    public N5CachedCellImg<T, ?> create(final int[] dimensions) {
        return create(Util.int2long(dimensions));
    }

    @SuppressWarnings("javadoc")
    public N5CachedCellImg<T, ?> create(final long[] dimensions, final N5CachedCellImgOptions additionalOptions) {
        return create(dimensions, null, null, type(), additionalOptions);
    }

    public N5CachedCellImg<T, ?> create(final Dimensions dimensions, final N5CachedCellImgOptions additionalOptions) {
        return create(Intervals.dimensionsAsLongArray(dimensions), additionalOptions);
    }

    public N5CachedCellImg<T, ?> create(final long[] dimensions, final CellLoader<T> loader) {
        return create(dimensions, null, loader, type(), null);
    }

    public N5CachedCellImg<T, ?> create(final Dimensions dimensions, final CellLoader<T> loader) {
        return create(Intervals.dimensionsAsLongArray(dimensions), null, loader, type(), null);
    }

    public N5CachedCellImg<T, ?> create(final long[] dimensions, final CellLoader<T> loader,
            final N5CachedCellImgOptions additionalOptions) {
        return create(dimensions, null, loader, type(), additionalOptions);
    }

    public N5CachedCellImg<T, ?> create(final Dimensions dimensions, final CellLoader<T> loader,
            final N5CachedCellImgOptions additionalOptions) {
        return create(Intervals.dimensionsAsLongArray(dimensions), null, loader, type(), additionalOptions);
    }

    public <A> N5CachedCellImg<T, A> createWithCacheLoader(final long[] dimensions,
            final CacheLoader<Long, Cell<A>> backingLoader) {
        return create(dimensions, backingLoader, null, type(), null);
    }

    public <A> N5CachedCellImg<T, A> createWithCacheLoader(final Dimensions dimensions,
            final CacheLoader<Long, Cell<A>> backingLoader) {
        return create(Intervals.dimensionsAsLongArray(dimensions), backingLoader, null, type(), null);
    }

    public <A> N5CachedCellImg<T, A> createWithCacheLoader(final long[] dimensions,
            final CacheLoader<Long, Cell<A>> backingLoader, final N5CachedCellImgOptions additionalOptions) {
        return create(dimensions, backingLoader, null, type(), additionalOptions);
    }

    public <A> N5CachedCellImg<T, A> createWithCacheLoader(final Dimensions dimensions,
            final CacheLoader<Long, Cell<A>> backingLoader, final N5CachedCellImgOptions additionalOptions) {
        return create(Intervals.dimensionsAsLongArray(dimensions), backingLoader, null, type(), additionalOptions);
    }

    class CreateData {
        public final CacheLoader<Long, ? extends Cell<?>> cacheLoader;

        public final CellLoader<T> cellLoader;

        public final T type;

        public final N5CachedCellImgOptions.Values options;

        public CreateData(final CacheLoader<Long, ? extends Cell<?>> cacheLoader, final CellLoader<T> cellLoader,
                final T type, final N5CachedCellImgOptions.Values options) {
            this.cacheLoader = cacheLoader;
            this.cellLoader = cellLoader;
            this.type = type;
            this.options = options;
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public <S> ImgFactory<S> imgFactory(final S type) throws IncompatibleTypeException {
        if (NativeType.class.isInstance(type))
            return new N5CachedCellImgFactory((NativeType) type, factoryOptions);
        throw new IncompatibleTypeException(this,
                type.getClass().getCanonicalName() + " does not implement NativeType.");
    }

    /**
     * Create image.
     *
     * @param dimensions        dimensions of the image to create.
     * @param cacheLoader       user-specified backing loader or {@code null}.
     * @param cellLoader        user-specified {@link CellLoader} or {@code null}.
     *                          Has no effect if {@code cacheLoader != null}.
     * @param type              type of the image to create
     * @param additionalOptions additional options that partially override general
     *                          factory options, or {@code null}.
     */
    private <A> N5CachedCellImg<T, A> create(final long[] dimensions,
            final CacheLoader<Long, ? extends Cell<? extends A>> cacheLoader, final CellLoader<T> cellLoader,
            final T type, final N5CachedCellImgOptions additionalOptions) {
        @SuppressWarnings({ "unchecked", "rawtypes" })
        final N5CachedCellImg<T, A> img = (N5CachedCellImg<T, A>)create(dimensions, cacheLoader, cellLoader, type, (NativeTypeFactory) type.getNativeTypeFactory(),
        additionalOptions);
        return img;
    }

    @Override
    AbstractReadWriteCachedCellImgOptions mergeWithFactoryOptions(
            AbstractReadWriteCachedCellImgOptions userProvidedOptions) {
        return (userProvidedOptions == null) ? factoryOptions : factoryOptions.merge(userProvidedOptions);
    }

    @Override
    protected <A extends ArrayDataAccess<A>> CachedCellImg<T, ? extends A> createCachedCellImg(CellGrid grid,
            Fraction entitiesPerPixel, Cache<Long, Cell<A>> cache, A accessType) {
        return new N5CachedCellImg<>(this, grid, entitiesPerPixel, cache, accessType);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <A extends ArrayDataAccess<A>> ReadWriteCellCache<A> createCellCache(
            AbstractReadWriteCachedCellImgOptions options, CellGrid grid, CacheLoader<Long, Cell<A>> backingLoader,
            T type, Fraction entitiesPerPixel) {
        N5CachedCellImgOptions.Values n5optionValues;
        if (options instanceof N5CachedCellImgOptions) {
            n5optionValues = ((N5CachedCellImgOptions)options).values();
        } else {
            // If the given options are no N5CachedCellImgOptions, we create default options for
            // the disk cache specifics, and merge them with the provided values.
            n5optionValues = N5CachedCellImgOptions.options().merge(options).values();
        }

        try {
            Path resultCacheLocation = n5optionValues.cacheDir();
            if (resultCacheLocation == null) {
                // fall back to tmp dir
                resultCacheLocation = Files.createTempDirectory("n5-cell-cache");
            }

            // TODO: handle n5optionValues.dirtyAccesses()
            return (ReadWriteCellCache<A>)(new N5CellCache<>(resultCacheLocation, n5optionValues.datasetName(), 
                    grid, backingLoader, entitiesPerPixel, AccessIo.get(type, n5optionValues.accessFlags()),
                    n5optionValues.compression(), type));
        }
        catch ( final IOException e )
		{
			throw new RuntimeException( e );
		}
    }

    /*
     * -----------------------------------------------------------------------
     *
     * Deprecated API.
     *
     * Supports backwards compatibility with ImgFactories that are constructed
     * without a type instance or supplier.
     *
     * -----------------------------------------------------------------------
     */
    @Deprecated
    public N5CachedCellImgFactory() {
        this(N5CachedCellImgOptions.options());
    }

    @Deprecated
    public N5CachedCellImgFactory(final N5CachedCellImgOptions optional) {
        this.factoryOptions = optional;
    }

    @Deprecated
    @Override
    public N5CachedCellImg<T, ?> create(final long[] dim, final T type) {
        cache(type);
        return create(dim, null, null, type, null);
    }

    @Deprecated
    public N5CachedCellImg<T, ?> create(final long[] dim, final T type, final N5CachedCellImgOptions additionalOptions) {
        cache(type);
        return create(dim, null, null, type, additionalOptions);
    }

    @Deprecated
    public N5CachedCellImg<T, ?> create(final long[] dim, final T type, final CellLoader<T> loader) {
        cache(type);
        return create(dim, null, loader, type, null);
    }

    @Deprecated
    public N5CachedCellImg<T, ?> create(final long[] dim, final T type, final CellLoader<T> loader,
            final N5CachedCellImgOptions additionalOptions) {
        cache(type);
        return create(dim, null, loader, type, additionalOptions);
    }

    @Deprecated
    public <A> N5CachedCellImg<T, A> createWithCacheLoader(final long[] dim, final T type,
            final CacheLoader<Long, Cell<A>> backingLoader) {
        cache(type);
        return create(dim, backingLoader, null, type, null);
    }

    @Deprecated
    public <A> N5CachedCellImg<T, A> createWithCacheLoader(final long[] dim, final T type,
            final CacheLoader<Long, Cell<A>> backingLoader, final N5CachedCellImgOptions additionalOptions) {
        cache(type);
        return create(dim, backingLoader, null, type, additionalOptions);
    }
}
