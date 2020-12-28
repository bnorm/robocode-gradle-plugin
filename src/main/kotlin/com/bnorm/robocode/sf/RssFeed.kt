package com.bnorm.robocode.sf

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement

@JacksonXmlRootElement(localName = "rss")
internal data class RssFeed(
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "channel")
    val channels: List<RssChannel>
)
