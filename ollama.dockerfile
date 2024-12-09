FROM ollama/ollama

# Copy the script to the docker image
COPY ./assistant.modelfile /assistant.modelfile
COPY ./startup.ollama.sh /startup.ollama.sh

# Ensure the script is executable
RUN chmod +x /startup.ollama.sh

EXPOSE 7869:11434
ENTRYPOINT ["/bin/sh", "/startup.ollama.sh"]