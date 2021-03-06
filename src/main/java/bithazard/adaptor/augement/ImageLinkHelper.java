package bithazard.adaptor.augement;

import bithazard.adaptor.augement.config.ImageLinkConfig;
import com.google.enterprise.adaptor.Response;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.awt.image.BufferedImage;
import java.net.URI;

public final class ImageLinkHelper {
    public static final String CONVERT_TO_PDF_PARAMETER = "convert2pdf=true";
    private static final String NBSP = "\u00a0";

    private ImageLinkHelper() {
        throw new AssertionError("Instantiating utility class...");
    }

    public static void setDisplayUrl(Response response, String url) {
        String originalUrl = url;
        if (url.contains("?" + CONVERT_TO_PDF_PARAMETER + "&") || url.contains("&" + CONVERT_TO_PDF_PARAMETER + "&")) {
            originalUrl = url.replace(CONVERT_TO_PDF_PARAMETER + "&", "");
        } else if (url.contains("&" + CONVERT_TO_PDF_PARAMETER)) {
            originalUrl = url.replace("&" + CONVERT_TO_PDF_PARAMETER, "");
        } else if (url.contains("?" + CONVERT_TO_PDF_PARAMETER)) {
            originalUrl = url.replace("?" + CONVERT_TO_PDF_PARAMETER, "");
        }
        response.setDisplayUrl(URI.create(originalUrl));
    }

    public static void addImageMetadata(Response response, BufferedImage image) {
        response.addMetadata("imageHeight", image.getHeight() + "");
        response.addMetadata("imageWidth", image.getWidth() + "");
        response.addMetadata("numberOfPixels", image.getWidth() * image.getHeight() + "");
        response.addMetadata("transparency", image.getColorModel().hasAlpha() + "");
        response.addMetadata("colorDepth", image.getColorModel().getPixelSize() + "");
    }

    public static void addImageLinksAsPdf(Document document, ImageLinkConfig imageLinkConfig, boolean tlsTermination) {
        if (imageLinkConfig == null) {
            return;
        }
        StringBuilder imageLinks = new StringBuilder("<!--googleoff: index--><!--googleoff: snippet-->");
        Elements images = document.select("img");
        for (Element image : images) {
            String imageUrl = image.attr("src");
            if (imageUrl.isEmpty()) {
                continue;
            }
            StringBuilder searchKeywords = new StringBuilder(image.attr("alt"));
            if (imageLinkConfig.isAddSurroundingText()) {
                String surroundingText = getSurroundingText(image, imageLinkConfig.getSurroundingTextMinLength());
                if (!surroundingText.equals(searchKeywords.toString())
                        && surroundingText.length() <= imageLinkConfig.getSurroundingTextMaxLength()) {
                    searchKeywords.append(" ").append(surroundingText);
                }
            }
            String pdfUrl = getPdfUrl(imageUrl, tlsTermination);
            imageLinks.append("<a href=\"").append(pdfUrl).append("\">").append(searchKeywords.toString()).append("</a>");
        }
        imageLinks.append("\n<!--googleon: snippet-->\n<!--googleon: index-->");
        document.body().append(imageLinks.toString());
    }

    private static String getSurroundingText(Element image, int surroundingTextMinLength) {
        Node nextNode = image;
        Node previousNode = image;
        while (nextNode != null && previousNode != null) {
            Node nextSiblingPeek = nextNode.nextSibling();
            if (nextSiblingPeek != null) {
                nextNode = nextSiblingPeek;
            } else {
                nextNode = nextNode.parent();
            }
            if (nextNode instanceof TextNode) {
                String text = ((TextNode) nextNode).text().replaceAll(NBSP, " ").trim();
                if (!text.isEmpty() && text.length() >= surroundingTextMinLength) {
                    return text;
                }
            } else if (nextNode instanceof Element) {
                String text = ((Element) nextNode).text().replaceAll(NBSP, " ").trim();
                if (!text.isEmpty() && text.length() >= surroundingTextMinLength) {
                    return text;
                }
            }
            Node previousSiblingPeek = previousNode.previousSibling();
            if (previousSiblingPeek != null) {
                previousNode = previousSiblingPeek;
            } else {
                previousNode = previousNode.parent();
            }
            if (previousNode instanceof TextNode) {
                String text = ((TextNode) previousNode).text().replaceAll(NBSP, " ").trim();
                if (!text.isEmpty() && text.length() >= surroundingTextMinLength) {
                    return text;
                }
            } else if (previousNode instanceof Element) {
                String text = ((Element) previousNode).text().replaceAll(NBSP, " ").trim();
                if (!text.isEmpty() && text.length() >= surroundingTextMinLength) {
                    return text;
                }
            }
        }
        return "";
    }

    private static String getPdfUrl(String imageUrl, boolean tlsTermination) {
        String modifiedImageUrl;
        if (tlsTermination) {
            modifiedImageUrl = getHttpUrl(imageUrl);
        } else {
            modifiedImageUrl = imageUrl;
        }
        if (modifiedImageUrl.endsWith("?") || modifiedImageUrl.endsWith("&")) {
            return modifiedImageUrl + CONVERT_TO_PDF_PARAMETER;
        }
        if (modifiedImageUrl.contains("?")) {
            return modifiedImageUrl + "&" + CONVERT_TO_PDF_PARAMETER;
        } else {
            return modifiedImageUrl + "?" + CONVERT_TO_PDF_PARAMETER;
        }
    }

    private static String getHttpUrl(String url) {
        if (url.startsWith("https://")) {
            return "http" + url.substring(5);
        }
        return url;
    }
}
