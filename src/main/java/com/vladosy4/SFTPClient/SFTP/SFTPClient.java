package com.vladosy4.SFTPClient.SFTP;

import com.jcraft.jsch.*;
import com.vladosy4.SFTPClient.JSON.JsonHandler;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class SFTPClient {
    private Session session;
    private ChannelSftp sftpChannel;
    private final String remoteFilePath = "infotecs.json";

    public void connect(String host, int port, String username, String password) throws JSchException {
        JSch jsch = new JSch();
        session = jsch.getSession(username, host, port);
        session.setPassword(password);

        session.setConfig("StrictHostKeyChecking", "no");

        System.out.println("Connecting to the SFTP server...");
        session.connect();
        System.out.println("The connection is established.");

        Channel channel = session.openChannel("sftp");
        channel.connect();
        sftpChannel = (ChannelSftp) channel;
        System.out.println("The SFTP channel is open.");
    }

    public String addDomainIpPair(String domain, String ip) throws SftpException, IOException {
        isConnected();

        System.out.println("Downloading a file from SFTP: " + remoteFilePath);

        try (InputStream inputStream = sftpChannel.get(remoteFilePath)) {
            InputStream updatedStream = JsonHandler.addDomainIpPair(inputStream, domain, ip);

            System.out.println("Uploading the updated JSON back to the server...");
            sftpChannel.put(updatedStream, remoteFilePath);
            return "The file has been successfully updated on the server.";

        } catch (IllegalArgumentException e) {
            return "Error: " + e.getMessage();
        }
    }

    public String deleteDomainIpPair(String identifier) throws SftpException, IOException {
        System.out.println("\n[INFO] Downloading a JSON file from the server...");

        InputStream inputStream = sftpChannel.get(remoteFilePath);
        String json = JsonHandler.readStream(inputStream);

        String updatedJson = JsonHandler.removeDomainIpPair(json, identifier);

        if (updatedJson.equals(json)) {
            return "[WARNING] The record was not found or the JSON has not changed.";
        }

        InputStream updatedStream = new ByteArrayInputStream(updatedJson.getBytes(StandardCharsets.UTF_8));
        sftpChannel.put(updatedStream, remoteFilePath);

        return "[SUCCESS] Pair deleted: " + identifier;
    }

    public void disconnect() {
        if (sftpChannel != null && sftpChannel.isConnected()) {
            sftpChannel.disconnect();
            System.out.println("The SFTP channel is closed.");
        }
        if (session != null && session.isConnected()) {
            session.disconnect();
            System.out.println("The session is disabled.");
        }
    }

    public String ipAddressByDomain(String domain) throws SftpException, IOException {
        isConnected();
        InputStream inputStream = sftpChannel.get(remoteFilePath);
        String ip = null;

        List<Map<String, String>> domainIpList = JsonHandler.readJson(inputStream);

        for (Map<String, String> map : domainIpList) {

            if (map.containsValue(domain)) {
                ip = map.get("ip");
            }
        }
        if (ip == null) {
            return "There's no information about IP address of this domain. (" + domain + ")";
        }
        return ip;
    }

    public String domainByIpAddress(String ip) throws SftpException, IOException {
        isConnected();
        InputStream inputStream = sftpChannel.get(remoteFilePath);
        String domain = null;

        List<Map<String, String>> domainIpList = JsonHandler.readJson(inputStream);

        for (Map<String, String> map : domainIpList) {

            if (map.containsValue(ip)) {
                domain = map.get("domain");
            }
        }
        if (domain == null) { return "There's no information about domain of this IP address. (" + ip + ")"; }
        return domain;
    }

    public List<Map<String, String>> listDomainIpPairs() throws SftpException, IOException {
        isConnected();
        InputStream inputStream = sftpChannel.get(remoteFilePath);
        List<Map<String, String>> domainIpList = JsonHandler.readJson(inputStream);
        return domainIpList;
    }

    public void isConnected() {
        if (sftpChannel == null || !sftpChannel.isConnected()) {
            throw new IllegalStateException("The SFTP connection is not established!");
        }
    }
}