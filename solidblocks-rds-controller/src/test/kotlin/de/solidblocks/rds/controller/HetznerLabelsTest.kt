package de.solidblocks.rds.controller

import de.solidblocks.rds.controller.utils.Constants.managedByLabel
import de.solidblocks.rds.controller.utils.Constants.versionLabel
import de.solidblocks.rds.controller.utils.HetznerLabels
import de.solidblocks.rds.shared.solidblocksVersion
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HetznerLabelsTest {

    @Test
    fun hasDefaultLabels() {

        val labels = HetznerLabels()

        assertThat(labels.labels()).hasSize(2)
        assertThat(labels.labels()[managedByLabel]).isEqualTo("true")
        assertThat(labels.labels()[versionLabel]).isEqualTo(solidblocksVersion())
    }

    @Test
    fun testAddHashedLabel() {

        val labels = HetznerLabels()

        labels.addHashedLabel("hash-test", "hallo welt")
        assertThat(labels.labels()["hash-test"]).isEqualTo("028fb9cd289c106642177d7bd4b6c5e107265b90f17f6b52a1cb0d7584264455")
    }

    @Test
    fun testMaxLabelValue() {
        val labels = HetznerLabels()
        labels.addLabel("label1", "A".repeat(124))
        assertThat(labels.labels()).hasSize(3)
        assertThat(labels.labels()["label1"]).isEqualTo("A".repeat(124))
    }

    @Test
    fun testTooLongLabelValue() {
        val labels = HetznerLabels()

        assertThrows(
            RuntimeException::class.java
        ) {
            labels.addLabel("label1", "A".repeat(125))
        }
    }

    @Test
    fun testUnderscoreInKey() {
        val labels = HetznerLabels()

        assertThrows(
            RuntimeException::class.java
        ) {
            labels.addLabel("label_1", "value1")
        }
    }
}
