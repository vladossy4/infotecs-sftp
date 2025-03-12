package com.vladosy4.SFTPClient;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import com.vladosy4.SFTPClient.SFTP.SFTPClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

public class Main {

    public static void main(String[] args) {
        String host;
        String port;
        String user;
        String password;

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        SFTPClient client = new SFTPClient();

        try {
            System.out.println("Enter server's address:");
            host = br.readLine();
            if (host.isEmpty()) { host = "185.58.207.63";   }
            System.out.println("Enter server's port:");
            System.out.println("If you want to use the standard one (22), press Enter");
            port = br.readLine();
            if (port.isEmpty()) { port = "22"; }
            System.out.println("Enter username:");
            user = br.readLine();
            if (user.isEmpty()) { user = "test";   }
            System.out.println("Enter user's password:");
            password = br.readLine();
            if (password.isEmpty()) { password = "password";   }
            client.connect(host, Integer.parseInt(port), user, password);
        } catch (IOException | JSchException e) {
            throw new RuntimeException(e);
        }

        while (true)
       {
           try {
               showMenu();
               int select = Integer.parseInt(br.readLine());

               switch (select) {
                   case 1: {
                       System.out.println("List of \"domain – ip address\" pairs:");
                       List<Map<String, String>> domainIpList = client.listDomainIpPairs();
                       for (Map<String, String> map : domainIpList) {
                           System.out.println(map.toString().substring(1, map.toString().length() - 1));
                       }
                       System.out.println("press Enter to exit menu");
                       br.readLine();
                       continue;
                   }
                   case 2: {
                       System.out.print("Enter domain name: ");
                       System.out.println(client.ipAddressByDomain(br.readLine()));
                       System.out.println("press Enter to exit menu");
                       br.readLine();
                       continue;
                   }
                   case 3:{
                       System.out.print("Enter ip address: ");
                       System.out.println(client.domainByIpAddress(br.readLine()));
                       System.out.println("press Enter to exit menu");
                       br.readLine();
                       continue;
                   }
                   case 4: {
                       System.out.println("Enter domain name:");
                       String newDomain = br.readLine();
                       System.out.println("Enter IP-address:");
                       String newIp = br.readLine();

                       try {
                           System.out.println(client.addDomainIpPair(newDomain, newIp));
                       } catch (Exception e) {
                           System.out.println("Error: " + e.getMessage());
                       }
                       continue;
                   }
                   case 5: {
                       System.out.println("Enter domain or IP to delete:");
                       String identifier = br.readLine().trim();

                       if (identifier.isEmpty()) {
                           System.out.println("[ERROR] Empty input! Please try again.");
                           continue;
                       }

                       System.out.println(client.deleteDomainIpPair(identifier));
                       continue;
                   }
                   case 6:
                       client.disconnect();
                       System.exit(0);
               }

           } catch (IOException | SftpException e) {
               throw new RuntimeException(e);
           }
       }
    }

    static void showMenu() {
        System.out.println("Welcome to the menu");
        System.out.println("1.\tGetting a list of \"domain – ip address\" pairs from a file");
        System.out.println("2.\tGetting an IP address by domain name");
        System.out.println("3.\tGetting a domain name by IP address");
        System.out.println("4.\tAdding a new \"domain – ip address\" pair to the file");
        System.out.println("5.\tDeleting the \"domain – ip address\" pair by domain name or IP address");
        System.out.println("6.\tExit");
        System.out.println();
        System.out.print("Enter your choice: ");
    }
}