package com.ru.scraper.service;

import com.ru.scraper.data.response.ResponseMenu;

import java.time.LocalDateTime;

public interface IScrapService {
    ResponseMenu scrape(LocalDateTime dateToScrap) throws InterruptedException;
}
