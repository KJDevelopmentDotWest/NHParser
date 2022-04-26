package com.example.nhparser.exception

class NHException: RuntimeException {
    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(throwable: Throwable) : super(throwable)
}