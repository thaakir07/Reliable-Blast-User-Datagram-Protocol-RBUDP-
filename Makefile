JAVAFX_HOME=~/javafx/javafx-sdk-23.0.2
SRC=src
BIN=bin

# Compiler flags for JavaFX
JAVAC_FLAGS=--module-path $(JAVAFX_HOME)/lib --add-modules javafx.controls,javafx.fxml
JAVA_FLAGS=--module-path $(JAVAFX_HOME)/lib --add-modules javafx.controls,javafx.fxml -cp $(BIN)

# Ensure bin directory exists
$(BIN):
	mkdir -p $(BIN)

# Compile Java files
Main: $(BIN)
	javac $(JAVAC_FLAGS) -d $(BIN) $(SRC)/Sender.java $(SRC)/Receiver.java $(SRC)/Main.java

# Run the application
run: Main
	java $(JAVA_FLAGS) -cp $(BIN) Main

# Clean up compiled files
clean:
	rm -rf $(BIN)
