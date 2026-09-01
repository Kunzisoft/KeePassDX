package com.kunzisoft.keepass.tests.template

import com.kunzisoft.keepass.database.element.template.TemplateAttributeOption
import junit.framework.TestCase
import org.junit.Assert

class TemplateAttributeOptionTest: TestCase() {

    fun testSerializeOptions() {
        val options = TemplateAttributeOption().apply {
            put("TestA", "TestB")
            put("{D", "}C")
            put("E,gyu", "15,jk")
            put("ù*:**", "78:96?545")
        }

        val strings = TemplateAttributeOption.getStringFromOptions(options)
        val optionsAfterSerialization = TemplateAttributeOption.getOptionsFrom(strings.toCharArray())
        val otherString = TemplateAttributeOption.getStringFromOptions(optionsAfterSerialization)

        Assert.assertEquals("Output not equal to input", strings, otherString)
    }

}