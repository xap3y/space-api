package me.xap3y.space.api.iface;

import me.xap3y.space.entity.User;
import org.jetbrains.annotations.Nullable;

public interface ApiResource {

    String getUniqueId();

    @Nullable User getUploader();
}
