package bithazard.adaptor.augement;

import bithazard.adaptor.augement.config.ImageLinkConfig;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.net.URI;

public class ImageLinkHelper {
    public static final String CONVERT_TO_PDF_PARAMETER = "convert2pdf=true";

    public static URI getOriginalUrl(final String url) {
        String originalUrl = url;
        if (url.contains("?" + CONVERT_TO_PDF_PARAMETER + "&") || url.contains("&" + CONVERT_TO_PDF_PARAMETER + "&")) {
            originalUrl = url.replace(CONVERT_TO_PDF_PARAMETER + "&", "");
        } else if (url.contains("&" + CONVERT_TO_PDF_PARAMETER)) {
            originalUrl = url.replace("&" + CONVERT_TO_PDF_PARAMETER, "");
        } else if (url.contains("?" + CONVERT_TO_PDF_PARAMETER)) {
            originalUrl = url.replace("?" + CONVERT_TO_PDF_PARAMETER, "");
        }
        return URI.create(originalUrl);
    }

    public static void addImageLinksAsPdf(final Document document, final ImageLinkConfig imageLinkConfig) {
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
            String pdfUrl = getPdfUrl(imageUrl);
            imageLinks.append("<a href=\"").append(pdfUrl).append("\">").append(searchKeywords.toString()).append("</a>");
        }
        imageLinks.append("\n<!--googleon: snippet-->\n<!--googleon: index-->");
        document.body().append(imageLinks.toString());
    }

    private static String getSurroundingText(final Element image, final int surroundingTextMinLength) {
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
                String text = ((TextNode) nextNode).text().replaceAll("\u00a0", " ").trim();
                if (!text.isEmpty() && text.length() >= surroundingTextMinLength) {
                    return text;
                }
            } else if (nextNode instanceof Element) {
                String text = ((Element) nextNode).text().replaceAll("\u00a0", " ").trim();
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
                String text = ((TextNode) previousNode).text().replaceAll("\u00a0", " ").trim();
                if (!text.isEmpty() && text.length() >= surroundingTextMinLength) {
                    return text;
                }
            } else if (previousNode instanceof Element) {
                String text = ((Element) previousNode).text().replaceAll("\u00a0", " ").trim();
                if (!text.isEmpty() && text.length() >= surroundingTextMinLength) {
                    return text;
                }
            }
        }
        return "";
    }

    private static String getPdfUrl(final String imageUrl) {
        if (imageUrl.endsWith("?") || imageUrl.endsWith("&")) {
            return imageUrl + CONVERT_TO_PDF_PARAMETER;
        }
        if (imageUrl.contains("?")) {
            return imageUrl + "&" + CONVERT_TO_PDF_PARAMETER;
        } else {
            return imageUrl + "?" + CONVERT_TO_PDF_PARAMETER;
        }
    }
}
