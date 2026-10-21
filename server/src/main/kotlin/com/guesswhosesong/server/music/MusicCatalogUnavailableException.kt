package com.guesswhosesong.server.music

class MusicCatalogUnavailableException(cause: Throwable? = null) :
    RuntimeException("Music catalog temporarily unavailable", cause)
