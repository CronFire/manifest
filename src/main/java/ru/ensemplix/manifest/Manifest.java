package ru.ensemplix.manifest;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.codec.digest.DigestUtils;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class Manifest {

    private final File folder;

    private final File output;

    private final boolean info;

    private final List<Resource> resources = new LinkedList<>();

    public Manifest(File folder, File output, boolean info) {
        this.folder = folder;
        this.output = output;
        this.info = info;
    }

    public List<Resource> getResources() {
        return resources;
    }

    private void fetch(File target) throws IOException {
        for(File file : target.listFiles()) {
            if(file.isDirectory()) {
                fetch(file);
                continue;
            }

            String name = file.getAbsolutePath().replace(folder.getAbsolutePath(), "").replaceAll("\\\\", "/");
            String hash = DigestUtils.md5Hex(new FileInputStream(file));
            long size = file.length();

            resources.add(new Resource(name, hash, size));

            if(info) {
                System.out.println("Name: " + name + ", Hash: " + hash +  ", Size: " + size);
            }
        }
    }

    public static void main(String[] args) throws ParseException, IOException {
        Options options = new Options();
        options.addOption("f", "folder", true, "folder that need to be patched");
        options.addOption("o", "output", true, "where save patch output");
        options.addOption("i", "info", false, "prints info during patch creation");

        CommandLine cmd = new GnuParser().parse(options, args);

        if(!cmd.hasOption("folder")) {
            System.out.println("Please provide folder that need to be patched.");
            return;
        }

        if(!cmd.hasOption("output")) {
            System.out.println("Please provide output file name");
            return;
        }

        File folder = new File(cmd.getOptionValue("folder"));

        if(!folder.exists()) {
            System.out.println("Please provide existing folder.");
            return;
        }

        File output = new File(cmd.getOptionValue("output"));

        System.out.println("Creating manifest.");

        Manifest manifest = new Manifest(folder, output, cmd.hasOption("info"));
        manifest.fetch(folder);

        System.out.println("Finished creating manifest patch from " + manifest.getResources().size() + " resources.");

        ObjectMapper mapper = new ObjectMapper();

        mapper.writerWithDefaultPrettyPrinter().writeValue(output, manifest.getResources());
    }

    public static class Resource {

        public String name;

        public String hash;

        public long size;

        public Resource() {

        }

        public Resource(String name, String hash, long size) {
            this.name = name;
            this.hash = hash;
            this.size = size;
        }

    }

}
