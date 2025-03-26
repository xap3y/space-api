package me.xap3y.space.controller;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.xap3y.space.SpaceApplication;
import me.xap3y.space.config.ServerInfo;
import me.xap3y.space.dto.NewImageDto;
import me.xap3y.space.dto.PasteDto;
import me.xap3y.space.dto.UrlDto;
import me.xap3y.space.mapper.PasteMapper;
import me.xap3y.space.mapper.UrlMapper;
import me.xap3y.space.service.ImageService;
import me.xap3y.space.service.PasteService;
import me.xap3y.space.service.UrlService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.FileNotFoundException;
import java.nio.file.Files;

@Slf4j
@Controller
@RequestMapping("/web")
public class WebController {

    private final ImageService imageService;
    private final PasteService pasteService;
    private final PasteMapper pasteMapper;
    private final UrlService urlService;
    private final UrlMapper urlMapper;
    private final ServerInfo serverInfo;

    public static final String ERROR_PAGE_BAD_REQUEST = "redirect:/error400";
    public static final String ERROR_PAGE_NOT_FOUND = "redirect:/error404";

    public WebController(ImageService imageService, PasteService pasteService, PasteMapper pasteMapper, UrlService urlService, UrlMapper urlMapper, ServerInfo serverInfo) {
        this.imageService = imageService;
        this.pasteService = pasteService;
        this.pasteMapper = pasteMapper;
        this.urlService = urlService;
        this.urlMapper = urlMapper;
        this.serverInfo = serverInfo;
    }

    @RequestMapping(
            value = "/error400"
    ) public String renderBadRequestErrorPage() {
        return "error400";
    }

    @RequestMapping(
            value = "/apidocs"
    ) public String renderApiDocs() {
        return "error500";
    }

    @RequestMapping(
            value = "/error404"
    ) public String renderResourceNotFoundErrorPage() {
        return "error404";
    }

    @RequestMapping(
            value = "/error500"
    ) public String renderError500() {
        return "error500";
    }

    // Render .jsp files
    @RequestMapping(
            value = "/image-upload"
    ) public String renderPage(Model model) {
        model.addAttribute("version", SpaceApplication.VERSION);
        log.info("Rendering image-upload page");
        return "upload";
    }

    @SneakyThrows
    @GetMapping(
            value = "/image-render/{id}"
    ) public String renderImage(
            @PathVariable String id,
            Model model
    ) {
        NewImageDto image;

        try {
            image = imageService.getImageStream(id, false, true);
        } catch (Exception e) {
            return "error404";
        }

        InputStreamResource fileResource = new InputStreamResource(Files.newInputStream(image.path()));
        String mimeType = Files.probeContentType(image.path());

        model.addAttribute("stream", fileResource);
        model.addAttribute("mimeType", mimeType);
        model.addAttribute("uploader", image.uploader().getUsername());
        model.addAttribute("link", serverInfo.getBaseUrl() + "/v1/image/get/" + id);
        model.addAttribute("portalurl", serverInfo.getFrontEndUrl() + "/image/" + id);

        return "render";
    }

    @GetMapping(
            value = "/image-render"
    ) public String renderImageDefault() {
        return "render-default";
    }

    @RequestMapping(
            value = "/paste-create"
    ) public String createPastePage(Model model) {
        return "createpaste";
    }

    @RequestMapping(
            value = "/url-create"
    ) public String createUrlPage(Model model) {
        return "createurl";
    }

    @GetMapping(
            value = "/paste-render/{id}"
    ) public String renderPaste(
            @PathVariable String id,
            Model model
    ) {
        PasteDto paste;

        try {
            paste = pasteMapper.apply(pasteService.getPasteByUniqueId(id)
                    .orElseThrow(() -> new FileNotFoundException("Paste not found")));

        } catch (Exception e) {
            return "error404";
        }

        model.addAttribute("paste", paste.content());
        model.addAttribute("uploader", paste.uploader());
        model.addAttribute("link", serverInfo.getBaseUrl() + "/v1/paste/get/" + id + "?raw=true");

        return "paste";
    }

    @GetMapping(
            value = "/url-render/{id}"
    ) public String renderUrl(
            @PathVariable String id,
            Model model
    ) {
        UrlDto urlDto;

        try {
            urlDto = urlMapper.apply(urlService.getUrlByUniqueId(id)
                    .orElseThrow(() -> new FileNotFoundException("Url not found")));

        } catch (Exception e) {
            return "error404";
        }

        model.addAttribute("code", urlDto.shortCode());
        model.addAttribute("uploader", urlDto.uploader());
        model.addAttribute("link", urlDto.url());

        return "shorener";
    }
}
