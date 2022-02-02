package me.laurens.FPL.api;

import java.io.FileWriter;
import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class writeJson {

	//Writes a JsonObject to a json file for viewing purposes.
	public static void writeJsonObject(JsonObject jsonObject, String path) throws IOException {

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		FileWriter fileWriter = new FileWriter(path);

		gson.toJson(jsonObject, fileWriter);
		fileWriter.close();

	}

	//Writes a JsonArray to a json file for viewing purposes.
	public static void writeJsonArray(JsonArray jsonArray, String path) throws IOException {

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		FileWriter fileWriter = new FileWriter(path);

		gson.toJson(jsonArray, fileWriter);
		fileWriter.close();

	}
}
