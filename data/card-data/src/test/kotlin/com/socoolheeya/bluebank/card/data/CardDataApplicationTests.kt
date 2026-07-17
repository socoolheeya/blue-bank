package com.socoolheeya.bluebank.card.data

import de.infix.testBalloon.framework.core.testSuite
import com.socoolheeya.bluebank.card.data.domain.CardEnums

val cardDataApplicationSuite by testSuite {
    test("card data module smoke test") {
        check(CardEnums.CardType.entries.isNotEmpty())
    }
}
