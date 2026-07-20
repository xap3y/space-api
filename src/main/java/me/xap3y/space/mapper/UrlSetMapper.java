package me.xap3y.space.mapper;

import lombok.AllArgsConstructor;
import me.xap3y.space.api.enums.UrlSetPreference;
import me.xap3y.space.api.iface.ApiResource;
import me.xap3y.space.config.ServerInfo;
import me.xap3y.space.dto.UrlSetDto;
import me.xap3y.space.entity.*;
import me.xap3y.space.service.UserSettingsService;
import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
@AllArgsConstructor
public class UrlSetMapper implements Function<ApiResource, UrlSetDto> {

    private final ServerInfo serverInfo;
    private final UserSettingsService userSettingsService;

    public UrlSetDto apply(ApiResource resource, Long userId) {
        String webUrl = null;
        String rawUrl = null;
        String portalUrl = null;
        String shortUrl = null;
        String customUrl = null;
        String deleteUrl = null;
        String userUrl = null;
        String posterUrl = null;

        UrlSetPreference preference = null;

        UserSettings userSettings;
        if (userId != null ) userSettings = userSettingsService.getUserSettingsByUserId(userId).orElse(null);
        else userSettings = null;

        switch (resource) {
            case Image img -> {
                rawUrl = switch (img.getLocation()) {
                    case R2 -> "https://r2.xap3y.eu/media/" + resource.getUniqueId();
                    case LOCAL -> serverInfo.getBaseUrl() + "/v1/image/get/" + resource.getUniqueId();
                    default -> serverInfo.getBaseUrl() + "/web/image-render/" + resource.getUniqueId();
                };

                webUrl = serverInfo.getBaseUrl() + "/web/image-render/" + resource.getUniqueId();
                portalUrl = serverInfo.getFrontEndUrl() + "/i/" + resource.getUniqueId();
                shortUrl = serverInfo.getShortImageUrl() + "/" + resource.getUniqueId();
                deleteUrl = serverInfo.getBaseUrl() + "/v1/image/get/" + resource.getUniqueId();
                if (userSettings != null && userSettings.getUrlSettings() != null && userSettings.getUrlSettings().getImage() != null) {
                    preference = userSettings.getUrlSettings().getImage();
                }
                if (img.isPoster()) {
                    posterUrl = serverInfo.getBaseUrl() + "/v1/image/get/poster/" + resource.getUniqueId();
                }
            }
            case Paste paste -> {
                rawUrl = serverInfo.getBaseUrl() + "/v1/paste/get/" + resource.getUniqueId();
                webUrl = serverInfo.getBaseUrl() + "/web/paste/" + resource.getUniqueId();
                portalUrl = serverInfo.getFrontEndUrl() + "/p/" + resource.getUniqueId();
                shortUrl = serverInfo.getShortPasteUrl() + "/" + resource.getUniqueId();
                deleteUrl = rawUrl;
                if (userSettings != null && userSettings.getUrlSettings() != null && userSettings.getUrlSettings().getPaste() != null) {
                    preference = userSettings.getUrlSettings().getPaste();
                }
            }
            case Url url -> {
                rawUrl = serverInfo.getBaseUrl() + "/v1/url/r/" + resource.getUniqueId();
                portalUrl = serverInfo.getFrontEndUrl() + "/r/" + resource.getUniqueId();
                shortUrl = serverInfo.getShortShortenerUrl() + "/" + resource.getUniqueId();
                deleteUrl = rawUrl;
                if (userSettings != null && userSettings.getUrlSettings() != null && userSettings.getUrlSettings().getUrl() != null) {
                    preference = userSettings.getUrlSettings().getUrl();
                }
            }
            case TranscriptImage trImage -> {
                rawUrl = switch (trImage.getLocation()) {
                    case R2 -> "https://r2.xap3y.eu/media/tr_images/" + resource.getUniqueId();
                    case LOCAL -> serverInfo.getBaseUrl() + "/v1/image/get/tr-" + resource.getUniqueId();
                    default -> serverInfo.getBaseUrl() + "/web/image-render/" + resource.getUniqueId();
                };
            }
            default -> {
            }
        }

        switch (preference) {
            case RAW -> userUrl = rawUrl;
            case SHORT -> userUrl = shortUrl;
            case null -> {}
            default -> userUrl = portalUrl;
        }

        return new UrlSetDto(
                webUrl,
                portalUrl,
                rawUrl,
                shortUrl,
                customUrl,
                deleteUrl,
                userUrl,
                posterUrl
        );
    }

    @Override
    public UrlSetDto apply(ApiResource resource) {

        return apply(resource, resource.getUploader() != null ? resource.getUploader().getId() : null);
    }
}
