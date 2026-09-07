package org.spendoo.statistics

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.spendoo.statistics.api.dto.response.CombinedStatsResponse
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.http.MediaType
import org.springframework.mock.http.MockHttpInputMessage

class CombinedStatsDeserializationTest {

    @Test
    fun `test deserialization of AI combined response`() {
        val json = """
        {
            "financial_stats_forecast": {
                "buckets": [
                    {
                        "spending": 150.50,
                        "income": 0.0,
                        "budget": 200.0,
                        "start_date": "2026-07-05T21:00:00Z",
                        "predicted": false,
                        "status": "WITHIN"
                    }
                ],
                "highest_spending_bucket_index": 0,
                "highest_value": 150.50,
                "predict": true
            },
            "budget_status": {
                "buckets": [
                    {
                        "spending": 150.50,
                        "status": "WITHIN",
                        "percentage": 75.25,
                        "start_date": "2026-07-05T21:00:00Z"
                    }
                ],
                "highest_spending": 150.50
            },
            "top_categories": {
                "total_spending": 150.50,
                "top_categories": [
                    {
                        "category_id": "01851ec9-7a36-458f-8788-80799bf95216",
                        "category_name": "Food",
                        "category_icon": "food_icon",
                        "spending": 150.50,
                        "percentage_change": 5.0,
                        "contribution_percentage": 100.0
                    }
                ]
            }
        }
        """.trimIndent()


        val converter = JacksonJsonHttpMessageConverter()
        val inputMessage = MockHttpInputMessage(json.toByteArray(Charsets.UTF_8))
        inputMessage.headers.contentType = MediaType.APPLICATION_JSON
        
        val result = converter.read(CombinedStatsResponse::class.java, inputMessage) as CombinedStatsResponse
        
        assertNotNull(result)
        assertEquals(0, result.financialStats.highestSpendingBucketIndex)
        assertEquals(true, result.financialStats.predicted)
        assertEquals(1, result.financialStats.buckets.size)
        assertEquals(1, result.budgetStatus.buckets.size)
        assertEquals(1, result.topCategories.topCategories.size)
    }
}



