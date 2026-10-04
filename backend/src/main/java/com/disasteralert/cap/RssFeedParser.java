package com.disasteralert.cap;

import org.springframework.stereotype.Component;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class RssFeedParser {

    public List<RssItem> parse(InputStream input) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        Document document = factory.newDocumentBuilder().parse(input);
        NodeList nodes = document.getElementsByTagName("item");
        List<RssItem> items = new ArrayList<>();

        for (int i = 0; i < nodes.getLength(); i++) {
            Element item = (Element) nodes.item(i);
            items.add(new RssItem(
                    childText(item, "title"),
                    childText(item, "description"),
                    childText(item, "link"),
                    childText(item, "guid"),
                    childText(item, "pubDate")
            ));
        }
        return items;
    }

    private String childText(Element parent, String name) {
        NodeList list = parent.getElementsByTagName(name);
        if (list.getLength() == 0) return "";
        return list.item(0).getTextContent() == null ? "" : list.item(0).getTextContent().trim();
    }
}
