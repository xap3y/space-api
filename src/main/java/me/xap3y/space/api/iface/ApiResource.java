package me.xap3y.space.api.iface;

import me.xap3y.space.entity.User;

public interface ApiResource {

    String getUniqueId();

    User getUploader();
}
