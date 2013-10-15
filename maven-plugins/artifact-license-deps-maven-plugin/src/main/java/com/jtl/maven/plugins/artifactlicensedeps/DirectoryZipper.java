package com.jtl.maven.plugins.artifactlicensedeps;

import com.google.common.collect.Lists;
import com.google.common.io.Closeables;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class DirectoryZipper {

  private final File zipFile;
  private final File zipSourceDirectory;
  private List<String> zipFileList = Lists.newArrayList();

  public DirectoryZipper(File zipFile, File zipSourceDirectory) {
    this.zipFile = zipFile;
    this.zipSourceDirectory = zipSourceDirectory;
  }

  public void zipIt() {
    byte[] buffer = new byte[1024];
    FileOutputStream fos = null;
    ZipOutputStream zos = null;
    try {
      generateZipFileList(zipSourceDirectory);
      if (zipFileList.isEmpty()) {
        return;
      }

      fos = new FileOutputStream(zipFile);
      zos = new ZipOutputStream(fos);

      System.out.println("Output to Zip : " + zipFile);

      for (String zipFile : zipFileList) {
        System.out.println("File Added : " + zipFile);
        ZipEntry ze = new ZipEntry(zipFile);
        zos.putNextEntry(ze);

        FileInputStream in = new FileInputStream(zipSourceDirectory + File.separator + zipFile);

        int len;
        while ((len = in.read(buffer)) > 0) {
          zos.write(buffer, 0, len);
        }
        in.close();
      }

      zos.closeEntry();
      zos.close();

      System.out.println("Done");
    } catch (IOException ex) {
      ex.printStackTrace();
    } finally {
      Closeables.closeQuietly(zos);
      Closeables.closeQuietly(fos);
    }
  }

  /**
   * Traverse a directory and get all files,
   * and add the file into fileList
   *
   * @param node file or directory
   */
  private void generateZipFileList(File node) {
    if (node.isFile()) {
      zipFileList.add(generateZipEntry(node.getAbsoluteFile().toString()));
    }
    if (node.isDirectory()) {
      String[] subNote = node.list();
      for (String filename : subNote) {
        generateZipFileList(new File(node, filename));
      }
    }
  }

  /**
   * Format the file path for zip
   *
   * @param file file path
   * @return Formatted file path
   */
  private String generateZipEntry(String file) {
    return file.substring(zipSourceDirectory.getAbsolutePath().length() + 1, file.length());
  }
}
