package shape_comparator.services;

import javax.annotation.Nonnull;

public class Type {

    @Nonnull
    public String className;

    @Nonnull
    public Integer encodedKey;

    @Nonnull
    public Integer instanceCount;

    @Nonnull
    public Integer getInstanceCount() {
        return instanceCount;
    }

    public void setInstanceCount(@Nonnull Integer instanceCount) {
        this.instanceCount = instanceCount;
    }

    @Nonnull
    public String getName() {
        return className;
    }

    public void setName(@Nonnull String name) {
        this.className = name;
    }

    @Nonnull
    public Integer getEncodedKey() {
        return encodedKey;
    }

    public void setEncodedKey(@Nonnull Integer encodedKey) {
        this.encodedKey = encodedKey;
    }
}