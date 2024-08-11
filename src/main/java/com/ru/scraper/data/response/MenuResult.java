package com.ru.scraper.data.response;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class MenuResult {
    private Element imgElement;
    private Elements tableRows;

    public MenuResult(Element imgElement) {
        this.imgElement = imgElement;
    }

    public MenuResult(Elements tableRows) {
        this.tableRows = tableRows;
    }

    public boolean isImage() {
        return imgElement != null;
    }

    public Element getImageElement() {
        return imgElement;
    }

    public Elements getTableRows() {
        return tableRows;
    }
}
