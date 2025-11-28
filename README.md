🌍 GDP & Population Data Agent

A Kotlin-based data ingestion and analysis agent that loads World Bank GDP, Population, and Agricultural Land datasets, stores them in a relational in-memory SQLite database, and allows users to ask natural language questions powered by an LLM (OpenAI API).

🚀 Features

📦 Automatic download and extraction of GDP, Population, and Agriculture datasets (from World Bank API).

🧮 Cleans and parses CSVs into structured data objects.

🗃️ Stores data in a relational in-memory SQLite database.

🤖 AI agent that converts natural language into SQL queries using the OpenAI API.

📊 Supports combined analytics such as GDP per capita and Agriculture land %.

✨ Human-readable output formatting (B = Billion, M = Million, K = Thousand).


🧩 Tech Stack
Component	Technology
Language	Kotlin
Build Tool	Gradle
Database	SQLite (in-memory)
AI Integration	OpenAI API
HTTP & CSV	OkHttp, OpenCSV
Env Loader (optional)	dotenv-kotlin

⚙️ Setup Instructions

1️⃣ Clone the repository
git clone https://github.com/zalesjan/gdp_world_bank_agent
cd gdp_agent

2️⃣ (Optional) Create a virtual .env file

Copy .env.example → .env and add your own key:

OPENAI_API_KEY=your_openai_api_key_here


If you don’t want to use .env, you can export the key manually:

Windows PowerShell

setx OPENAI_API_KEY "your_openai_api_key_here"


macOS / Linux

export OPENAI_API_KEY="your_openai_api_key_here"

3️⃣ Build the project
./gradlew build

4️⃣ Run the program
./gradlew run


On first run, it will:

Download World Bank GDP, Population, and Agriculture datasets

Parse and store them in SQLite

Compute GDP per capita

Launch an interactive prompt for your questions

🧠 Example usage
> Which country had the highest GDP in 2020?
🤖 Generated SQL:
SELECT c.name, e.value AS gdp
FROM countries c
JOIN economy e ON e.country_id = c.id
WHERE e.indicator_code='GDP' AND e.year=2020
ORDER BY e.value DESC
LIMIT 1;

📊 Results:
{country=United States, gdp=23.00B}

📊 Example data output
{country=Monaco, year=2000, gdp=6.73B, population=38K, gdp_per_capita=177K}
{country=Ireland, year=2000, gdp=436.56B, population=5.04M, gdp_per_capita=87K}

🧩 Integration with a BI Stack

This project could be easily extended into a BI pipeline:

Replace in-memory SQLite with a persistent Postgres/Snowflake warehouse.

Add a REST API layer using Ktor or Spring Boot.

Feed the processed data into Power BI, Tableau, or Qlik for visualization.

Use the same ingestion + validation layer in a scheduled ETL job.

🧱 Limitations & Improvements
Area	Limitation	Possible Improvement
LLM interpretation	Relies on OpenAI for SQL mapping	Add rule-based parser for offline fallback
Data freshness	Static World Bank snapshot	Add periodic re-download or caching
Country name matching	Simple substring filter	Use ISO codes for consistent joins
Output	CLI only	Add a small web dashboard (Ktor or React)
Performance	In-memory DB only	Use persistent Postgres for scalability
👨‍💻 Author

Jan Zalesak
Data Engineer