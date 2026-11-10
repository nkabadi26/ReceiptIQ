# ReceiptIQ

ReceiptIQ extracts receipt data, stores receipt embeddings in PostgreSQL, and answers receipt questions with SQL totals or retrieval-augmented generation.

## What it does

- Processes receipt images and uses Gemini structured output to extract receipt and line-item data.
- Stores receipts, line items, and 768-dimensional Gemini embeddings in PostgreSQL with pgvector.
- Searches similar line items and answers natural-language receipt questions with RAG.
- Provides total paid amounts by merchant and optional date range, plus a smart router that selects SQL or RAG.

## Architecture

Receipt image -> Gemini structured output -> PostgreSQL receipt and line-item storage. Question -> smart router -> SQL total or RAG answer from vector search results.

## Tech stack

- Java 25
- Spring Boot 4.1.1
- Spring AI 2.0.1
- Google GenAI chat and embedding models
- PostgreSQL with pgvector and Hibernate Vector
- Maven

## How to run

1. Start PostgreSQL with pgvector:

   ```powershell
   docker run --name receiptiq-postgres -e POSTGRES_PASSWORD=<database-password> -e POSTGRES_DB=postgres -p 5432:5432 -d pgvector/pgvector:pg16
   ```

2. Enable the pgvector extension in PostgreSQL:

   ```sql
   CREATE EXTENSION IF NOT EXISTS vector;
   ```

3. Set the required environment variables in the VS Code Java launch configuration (`.vscode/launch.json`). 
    Copy .vscode/launch.example.json to .vscode/launch.json and fill in your values.

    Replace the placeholders with your own values:
      - `DB_PASSWORD`: the PostgreSQL password configured for the database container
      - `GEMINI_API_KEY`: your Gemini API key
      - `RECEIPT_DIRECTORY`: the absolute path to the local directory containing receipt images

4. Start the `ReceiptIqApplication` configuration from VS Code using Run and Debug. The launch configuration supplies the environment variables required by `application.yaml`.

   A shell start can also be used, but the environment variables must be set in that shell before running:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

## Example requests

Before querying receipts, load receipts first. Supported image formats are `.jpg`, `.jpeg`, and `.png`. 

- Load one receipt from a file in the configured `RECEIPT_DIRECTORY`:

  ```text
  GET http://localhost:8080/receipt/getLLMReceiptJson?fileName=folder/receipt.jpg
  ```

- Or load all supported receipt images in a directory under `RECEIPT_DIRECTORY`:

  ```text
  GET http://localhost:8080/receipt/getAllReceiptsFromFolder?folderPath=folder
  ```

- Ask a receipt question with RAG:

  ```text
  GET http://localhost:8080/api/ask?q=What%20snacks%20did%20I%20buy?
  ```

- Get the total paid for a merchant and optional date range:

  ```text
  GET http://localhost:8080/api/total?merchant=Walmart&from=2026-08-01&to=2026-08-31
  ```

- Let the smart router choose SQL or RAG:

  ```text
  GET http://localhost:8080/api/smart-ask?q=How%20much%20did%20I%20spend%20at%20Walmart?
  ```

- Backfill embeddings by current model:

  ```powershell
  curl.exe -X POST http://localhost:8080/api/admin/backfill-embeddings
  ```

## Design decisions and known limits

TODO

Note: 
  The folder endpoint has no duplicate check. Loading the same folder twice creates duplicate receipts.
  Data lives inside the container. If the container is removed, all receipts and vectors are lost. Add a volume (for example -v receiptiq-data:/var/lib/postgresql/data) to keep the data.


## License

Copyright (c) 2026 Narayan Kabadi. All rights reserved. Contact kabaditech@gmail.com for permission to use.