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
        Elements images = document.select("img");
        for (Element image : images) {
            String imageUrl = image.attr("href");
            if (imageUrl.isEmpty()) {
                continue;
            }
            String pdfUrl = getPdfUrl(imageUrl);
            Element pdfLink = image.after("<!--googleoff: index--><!--googleoff: snippet--><a href=\"" + pdfUrl
                    + "\"></a>\n<!--googleon: snippet-->\n<!--googleon: index-->");
            StringBuilder surroundingText = new StringBuilder(image.attr("alt"));
            if (imageLinkConfig.isAddSurroundingText()) {
                appendSurroundingText(image, surroundingText);
            }
            pdfLink.text(surroundingText.toString());
        }
    }

    private static void appendSurroundingText(final Element image, final StringBuilder surroundingText) {
        Node nextNode = image;
        Node previousNode = image;
        boolean textAppended = false;
        while (!textAppended) {
            nextNode = nextNode.nextSibling();
            if (nextNode instanceof TextNode) {
                String text = ((TextNode) nextNode).text();
                if (!text.isEmpty()) {
                    surroundingText.append(" ").append(text);
                    textAppended = true;
                }
            } else if (nextNode instanceof Element) {
                String text = ((Element) nextNode).text();
                if (!text.isEmpty()) {
                    surroundingText.append(" ").append(text);
                    textAppended = true;
                }
            }
            if (!textAppended) {
                previousNode = previousNode.previousSibling();
                if (previousNode instanceof TextNode) {
                    String text = ((TextNode) previousNode).text();
                    if (!text.isEmpty()) {
                        surroundingText.append(" ").append(text);
                        textAppended = true;
                    }
                } else if (previousNode instanceof Element) {
                    String text = ((Element) previousNode).text();
                    if (!text.isEmpty()) {
                        surroundingText.append(" ").append(text);
                        textAppended = true;
                    }
                }
            }
        }
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
