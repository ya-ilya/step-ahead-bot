#!/bin/bash

# Start Ollama in the background.
ollama serve &
# Record Process ID.
pid=$!

# Pause for Ollama to start.
sleep 5

echo "🔴 Retrieving llama3.1 model..."
ollama pull llama3.1:8b
echo "🟢 Done!"

# echo "🔴 Retrieving mxbai-embed-large model..."
# ollama pull mxbai-embed-large
# echo "🟢 Done!"

echo "🔴 Creating assistant model..."
ollama create assistant -f assistant.modelfile
echo "🟢 Done!"

# Wait for Ollama process to finish.
wait $pid