# WikiCrawl Data

## Summary:
This document focuses on the data part of the WikiCrawl Program, which includes the following: Grafana dashboard, database schema, PostgreSQL access, and data licensing.

# Accessing Grafana dashboard
The grafana dashboard can be access on a web browser using localhost:3000/ in which a username and password is prompted. The username is admin while the password is the ```GRAFANA_PASSWORD``` that was defined inside the .env file in the root of the project directory during the WikiCrawl setup phase. Inside the dashboard tab there will be one main dashboard containing multiple panels displaying data such as the distribution of sentiments, top entiies found, and pages crawled over time. **The panels in the dashboard will not appear unless there is data gathered from crawling English Wikipedia articles.**
<br>
![Grafana Dashboard](images/Grafana%20Dashboard%20View.png)
</br>

# Database Schema
The PostgreSQL database for WikiCrawl consists of three tables defined using SQLAlchemy with their own schemas and are structured as the following:

### Table: `crawled_page`

| Column Name | Data Type | Constraints / Notes |
| :--- | :--- | :--- |
| `id` | BigInteger | Primary Key, Indexed |
| `url` | Text | Not Null |
| `title` | Text | |
| `url_content` | Text | |
| `crawled_at` | DateTime | |
| `depth` | Integer | |

### Table: `page_analysis`

| Column Name | Data Type | Constraints / Notes |
| :--- | :--- | :--- |
| `id` | BigInteger | Primary Key, Indexed |
| `page_id` | BigInteger | Foreign Key (`crawled_page.id`) |
| `page_summary` | Text | |
| `page_sentiment` | String | |
| `analyzed_at` | DateTime | |

### Table: `page_entities`

| Column Name | Data Type | Constraints / Notes |
| :--- | :--- | :--- |
| `id` | BigInteger | Primary Key, Indexed |
| `page_id` | BigInteger | Foreign Key (`crawled_page.id`) |
| `entity_word` | String | |
| `entity_type` | String | |

# Accessing the PostgreSQL database

To directly access the data that was defined by the database schema above, run the following command while the docker nlp-service is running to gain access: ```docker compose exec db psql -U ${POSTGRES_USER} -d ${POSTGRES_DB}```. POSTGRES_USER and POSTGRES_DB were values that were defined inside the .env file in the root of the project directory during the setup phase, so its best to manually type them in the command

## Data Licensing

The data collected by this web crawler is sourced exclusively from [Wikipedia](https://www.wikipedia.org/). 

As such, all scraped textual content and associated metadata are governed by Wikipedia's licensing terms and are distributed under the **Creative Commons Attribution-ShareAlike 4.0 International License (CC BY-SA 4.0)**. 

**Under the terms of this license:**
* **Attribution:** Credit must be given to Wikipedia and its contributors as the original source of the content. The original URLs to the scraped pages are preserved in the `crawled_page` table to maintain this attribution.
* **ShareAlike:** If you remix, transform, or build upon this dataset, you must distribute your contributions under the same CC BY-SA 4.0 license.

For more information on Wikipedia's copyright policies, please refer to the [Wikipedia:Copyrights](https://en.wikipedia.org/wiki/Wikipedia:Copyrights) page. The source code for the crawler itself is licensed separately.