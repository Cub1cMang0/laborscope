# WikiCrawl 
A target-specific NLP web crawler to gather data from English Wikipedia articles to generate data to be aggregated.

## Note:
This is a mini version of the main laborscope project that can only crawl and gather data from, well, Wikipedia. The creation of this mini version was due to the shift in my priorities as I entered my final year of university; putting the main project on indefinite hiatus until I decide to return to developing the project. Lastly, internally, the code still utilizes the laborscope package and folder names because WikiCrawl is a sort of snapshot of that project.

# 1. Setup

After downloading the zip file and extracting it, open the ```.env.example``` file located in the root of the project folder and begin filling in the necessary credentials for the crawling process. Once the .env.example file has all the credentials filled out, save the file and rename it to ```.env```. The LaborScopeApplication extracts the .env file automatically, so there is no need to export it before running the main springboot program.

# 2. Building the nlp-service
In order for crawled data to be stored and analyzed, the nlp-service docker serviec needs to be built. In the root of the project directory, run the command ```docker compose up -d --wait```. The purpose of this command is to not only to build, but to return and run the nlp-service in the background while we run the main crawler program.

# 3. Running the main crawler program

To begin the crawling process, Maven needs to compile and run the java spring boot application. Enter the ```laborscope``` directory in the root of the project and run the command ```./mvnw spring-boot:run```. Once that command is executed the spring boot application will begin and will provide the following information: 
<br>
<br>
LaborScope is ready. Begin a crawl session with:
</br>
```curl -X POST "localhost:8080/crawl?url=https://en.wikipedia.org/wiki/Apache_Kafka"```
<br>
omit ?url=... to use the default seed. Press Ctrl+C to stop.
</br>
<br>
The url field is for the user to fill out with a desired English Wikipedia article to begin crawling. The terminal will output the links that Kafka has received, when crawling begins for a given link, and whether or not it will be skipped due to it being found in the cache or that its alredy in the database. 

# 4. Stopping the program process

When crawling is needed to stop, pressing ```Ctrl+C``` will stop the spring boot crawler in a swift manner. Once that is completed, the nlp-service also need to come to a stop as well by running the command ```docker compose stop```, which will stop all the processes running inside the docker nlp-service.

# Other Documentation

Further documentation of WikiCrawl can be found in the ```/docs``` directory such as the architecture of the system and how to view and or modify the data crawled.

# Responsible Use

WikiCrawl is meant for small, polite crawls. Its safeguards only work if you leave them on:

- **Follow Wikipedia's rules.** The crawler obeys `robots.txt` and waits between requests (500 ms by default). Don't lower the delay, don't run several instances at once (the limit applies per process), and keep `max-depth` small.
- **Identify yourself.** Set `CRAWLER_USER_AGENT` in your`.env` to a descriptive name with your own contact information (a repo URL or an email address), as Wikimedia asks of automated clients.
- **Use dumps for bulk data.** If you need large amounts of Wikipedia text, use [Wikimedia's database dumps](https://dumps.wikimedia.org/) or the official API instead of crawling.
- **Wikipedia only.** The crawler rejects other sites on purpose, because its robots.txt handling and link rules are written for Wikipedia. Don't adapt it to another site without handling that site's robots.txt and terms of use.
- **Treat the analysis as a rough signal.** Summaries, sentiment and entities come from general-purpose models and can be wrong (the sentiment model was trained on tweets). Don't use them to make decisions about people.
- **Respect the data license.** Wikipedia text is licensed under [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/). Keep the source URL (stored with every page), credit Wikipedia, and follow the share-alike terms for any text you redistribute. See [docs/data.md](docs/data.md) for details.

# License

The code in this repository is released under the [MIT License](LICENSE). That license covers this project's code only. It does not cover the crawled Wikipedia content (CC BY-SA 4.0), or the Hugging Face models, which are downloaded when the NLP service first starts and carry their own licenses (check each model's page before redistributing or using them commercially).