package com.vladosy4.SFTPClient.JSON;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class JsonHandler {

    public static InputStream addDomainIpPair(InputStream jsonStream, String domain, String ip) {
        System.out.println("[INFO] Adding a new pair: " + domain + " - " + ip);

        String json = new BufferedReader(new InputStreamReader(jsonStream))
                .lines().collect(Collectors.joining("\n"));


        int startIndex = json.indexOf("\"addresses\"");
        if (startIndex == -1) {
            throw new IllegalArgumentException("[ERROR] JSON does not contain the 'addresses' key!");
        }

        startIndex = json.indexOf("[", startIndex);
        int endIndex = json.lastIndexOf("]");

        if (startIndex == -1 || endIndex == -1) {
            throw new IllegalArgumentException("[ERROR] JSON does not contain an array of 'addresses'!");
        }

        String records = json.substring(startIndex + 1, endIndex).trim();

        if (records.contains("\"domain\": \"" + domain + "\"") || records.contains("\"ip\": \"" + ip + "\"")) {
            throw new IllegalArgumentException("[WARNING] The domain or IP already exists in JSON!");
        }

        if (!isValidIPv4(ip)) {
            throw new IllegalArgumentException("[ERROR] Invalid IP address!");
        }

        String newEntry = String.format("{\n      \"domain\": \"%s\",\n      \"ip\": \"%s\"\n    }", domain, ip);

        StringBuilder newJson = new StringBuilder(" {\n  \"addresses\": [\n");

        if (records.isEmpty()) {
            newJson.append("    ").append(newEntry).append("\n");
        } else {
            newJson.append("    ").append(records).append(",\n    ").append(newEntry).append("\n");
        }

        newJson.append("  ]\n}");

        System.out.println("[SUCCESS] The addition is completed!");

        return new ByteArrayInputStream(newJson.toString().getBytes(StandardCharsets.UTF_8));
    }

    public static String readStream(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        return sb.toString().trim();
    }


    public static boolean isValidIPv4(String ip) {
        String[] parts = ip.split("\\.");
        if (parts.length != 4) return false;
        for (String part : parts) {
            try {
                int num = Integer.parseInt(part);
                if (num < 0 || num > 255) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    public static String removeDomainIpPair(String json, String identifier) {
        System.out.println("[INFO] Starting to delete the record: " + identifier);

        int startIndex = json.indexOf("\"addresses\"");
        if (startIndex == -1) {
            System.out.println("[ERROR] JSON does not contain the 'addresses' key! Check the JSON:");
            return json;
        }

        startIndex = json.indexOf("[", startIndex);
        int endIndex = json.lastIndexOf("]");

        if (startIndex == -1 || endIndex == -1) {
            System.out.println("[ERROR] JSON does not contain an array of 'addresses'!");
            return json;
        }

        String records = json.substring(startIndex + 1, endIndex).trim();

        Pattern pattern = Pattern.compile("\\{[^}]+}");
        Matcher matcher = pattern.matcher(records);

        StringBuilder newJson = new StringBuilder("{\n  \"addresses\": [\n");
        boolean removed = false;

        while (matcher.find()) {
            String entry = matcher.group();

            if (entry.contains("\"domain\": \"" + identifier + "\"") || entry.contains("\"ip\": \"" + identifier + "\"")) {
                removed = true;
                continue;
            }

            newJson.append("    ").append(entry).append(",\n");
        }

        if (!removed) {
            System.out.println("[WARNING] The record was not found: " + identifier);
            return json;
        }

        int lastComma = newJson.lastIndexOf(",");
        if (lastComma != -1) {
            newJson.deleteCharAt(lastComma);
        }

        newJson.append("\n  ]\n}");

        System.out.println("[SUCCESS] Deletion is complete!");

        return newJson.toString();
    }

    public static List<Map<String, String>> readJson(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder jsonContent = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            jsonContent.append(line.trim());
        }
        reader.close();

        return parseJson(jsonContent.toString());
    }

    private static List<Map<String, String>> parseJson(String json) {
        List<Map<String, String>> addresses = new ArrayList<>();

        json = json.replaceAll("[\\n\\t]", "").trim();
        if (!json.startsWith("{") || !json.endsWith("}")) {
            throw new IllegalArgumentException("Invalid JSON format");
        }

        int start = json.indexOf("[");
        int end = json.lastIndexOf("]");
        if (start == -1 || end == -1) {
            throw new IllegalArgumentException("The 'addresses' array was not found");
        }

        String data = json.substring(start + 1, end).trim();
        String[] entries = data.split("},\\{");

        for (String entry : entries) {
            entry = entry.replace("{", "").replace("}", "").replace("\"", "").trim();
            String[] fields = entry.split(",");

            Map<String, String> map = new HashMap<>();
            for (String field : fields) {
                String[] pair = field.split(":");
                if (pair.length == 2) {
                    map.put(pair[0].trim(), pair[1].trim());
                }
            }
            addresses.add(map);
        }

        return addresses;
    }
}