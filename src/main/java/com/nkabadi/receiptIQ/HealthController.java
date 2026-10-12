package com.nkabadi.receiptIQ;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }

    @GetMapping("/add")
    public Map<String, Integer> add(
            @RequestParam(defaultValue = "0") int first,
            @RequestParam(defaultValue = "0") int second) {
        return Map.of(
                "first", first,
                "second", second,
                "result", first + second
        );
    }

    @GetMapping(value = {"/", "/calculator"}, produces = MediaType.TEXT_HTML_VALUE)
    public String calculator() {
        return """
                <!DOCTYPE html>
                <html lang=\"en\">
                <head>
                    <meta charset=\"UTF-8\">
                    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">
                    <title>ReceiptIQ Calculator</title>
                    <style>
                        body {
                            font-family: Arial, sans-serif;
                            display: flex;
                            align-items: center;
                            justify-content: center;
                            min-height: 100vh;
                            margin: 0;
                            background: #f4f7fb;
                        }
                        .card {
                            background: white;
                            border-radius: 12px;
                            box-shadow: 0 10px 25px rgba(0,0,0,0.1);
                            padding: 24px;
                            width: 360px;
                        }
                        h1 {
                            margin-top: 0;
                            font-size: 1.5rem;
                        }
                        .field {
                            display: flex;
                            flex-direction: column;
                            margin-bottom: 12px;
                        }
                        input {
                            padding: 10px 12px;
                            font-size: 1rem;
                            border: 1px solid #d0d7de;
                            border-radius: 8px;
                        }
                        .actions {
                            display: flex;
                            gap: 10px;
                            margin-top: 12px;
                        }
                        button {
                            flex: 1;
                            padding: 10px;
                            border: none;
                            border-radius: 8px;
                            color: white;
                            background: #2563eb;
                            cursor: pointer;
                            font-weight: 600;
                        }
                        button:hover {
                            background: #1d4ed8;
                        }
                        #pingButton {
                            background: #16a34a;
                        }
                        #pingButton:hover {
                            background: #15803d;
                        }
                        #result {
                            margin-top: 18px;
                            min-height: 24px;
                            font-weight: bold;
                            color: #1f2937;
                        }
                    </style>
                </head>
                <body>
                    <div class=\"card\">
                        <h1>ReceiptIQ Utility</h1>

                        <div class=\"field\">
                            <label for=\"firstNumber\">First number</label>
                            <input id=\"firstNumber\" type=\"number\" value=\"0\" />
                        </div>

                        <div class=\"field\">
                            <label for=\"secondNumber\">Second number</label>
                            <input id=\"secondNumber\" type=\"number\" value=\"0\" />
                        </div>

                        <div class=\"actions\">
                            <button id=\"pingButton\" type=\"button\">Ping</button>
                            <button id=\"addButton\" type=\"button\">Add numbers</button>
                        </div>

                        <div id=\"result\">Ready</div>
                    </div>

                    <script>
                        const pingButton = document.getElementById('pingButton');
                        const addButton = document.getElementById('addButton');
                        const result = document.getElementById('result');

                        pingButton.addEventListener('click', async () => {
                            const response = await fetch('/ping');
                            const text = await response.text();
                            result.textContent = 'Ping result: ' + text;
                        });

                        addButton.addEventListener('click', async () => {
                            const first = Number(document.getElementById('firstNumber').value || 0);
                            const second = Number(document.getElementById('secondNumber').value || 0);
                            const response = await fetch('/add?first=' + first + '&second=' + second);
                            const data = await response.json();
                            result.textContent = 'Sum result: ' + data.result;
                        });
                    </script>
                </body>
                </html>
                """;
    }
}
