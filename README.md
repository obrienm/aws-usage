



A decent query

GET aws-usage/billing/_search
{
  "query": {
    "filtered": {
      "query": {
        "match_all": {}
      },
      "filter": {
        "and": {
          "filters": [
            {
              "range": {
                "usage.dt": {
                  "from": "2014-01-01T00:00:00.000Z",
                  "to": "2015-12-31T00:00:00.000Z",
                  "include_lower": true,
                  "include_upper": false
                }
              }
            }
          ]
        }
      }
    }
  },
  "aggregations": {
    "histo": {
      "date_histogram": {
        "field": "usage.dt",
        "interval": "1d"
      },
      "aggregations": {
        "cost_per_hour_unblended": {
          "sum": {
            "field": "cost.unblended"
          }
        }
      }
    },
    "cost_unblended": {
      "terms": {
        "field": "productName"
      },
      "aggregations": {
        "total_cost_per_product_unblended": {
          "sum": {
            "field": "cost.unblended"
          }
        }
      }
    },
    "total_cost_unblended": {
      "sum": {
        "field": "cost.unblended"
      }
    }
  }
}


