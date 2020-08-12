package com.github.iryabov.invest.service.impl

import java.lang.RuntimeException

class NotEnoughFundsException(message: String): RuntimeException(message)