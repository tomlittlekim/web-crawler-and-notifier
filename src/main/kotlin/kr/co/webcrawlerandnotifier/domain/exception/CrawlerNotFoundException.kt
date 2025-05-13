package kr.co.webcrawlerandnotifier.domain.exception

import java.util.UUID

class CrawlerNotFoundException(id: UUID) : RuntimeException("Crawler not found with id: $id")