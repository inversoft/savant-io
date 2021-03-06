/*
 * Copyright (c) 2014-2018, Inversoft Inc., All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package org.savantbuild.io.zip;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipOutputStream;
import org.savantbuild.io.Directory;
import org.savantbuild.io.FileInfo;
import org.savantbuild.io.FileSet;
import org.savantbuild.io.FileTools;

/**
 * Helps build Zip files.
 *
 * @author Brian Pontarelli
 */
public class ZipBuilder {
  public final List<Directory> directories = new ArrayList<>();

  public final Path file;

  public final List<FileSet> fileSets = new ArrayList<>();

  public ZipBuilder(String file) {
    this(Paths.get(file));
  }

  public ZipBuilder(Path file) {
    this.file = file;
  }

  public int build() throws IOException {
    if (Files.exists(file)) {
      Files.delete(file);
    }

    if (!Files.isDirectory(file.getParent())) {
      Files.createDirectories(file.getParent());
    }

    // Sort the file infos and add the directories
    Set<FileInfo> fileInfos = new TreeSet<>();
    for (FileSet fileSet : fileSets) {
      Set<Directory> dirs = fileSet.toDirectories();
      dirs.removeAll(directories);
      for (Directory dir : dirs) {
        directories.add(dir);
      }

      fileInfos.addAll(fileSet.toFileInfos());
    }

    int count = 0;

    try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(file))) {
      for (Directory directory : directories) {
        String name = directory.name;
        ZipEntry entry = new ZipEntry(name.endsWith("/") ? name : name + "/");
        if (directory.mode != null) {
          entry.setUnixMode(FileTools.toMode(directory.mode));
        }
        zos.putNextEntry(entry);
        zos.closeEntry();
        count++;
      }

      for (FileInfo fileInfo : fileInfos) {
        ZipEntry entry = new ZipEntry(fileInfo.relative.toString());
        entry.setLastAccessTime(fileInfo.lastAccessTime);
        entry.setLastModifiedTime(fileInfo.lastModifiedTime);
        entry.setSize(fileInfo.size);
        entry.setUnixMode(fileInfo.toMode());
        zos.putNextEntry(entry);
        Files.copy(fileInfo.origin, zos);
        zos.flush();
        zos.closeEntry();
        count++;
      }
    }

    return count;
  }

  public ZipBuilder directory(Directory directory) throws IOException {
    directories.add(directory);
    return this;
  }

  public ZipBuilder fileSet(Path directory) throws IOException {
    return fileSet(new FileSet(directory));
  }

  public ZipBuilder fileSet(String directory) throws IOException {
    return fileSet(Paths.get(directory));
  }

  public ZipBuilder fileSet(FileSet fileSet) throws IOException {
    if (Files.isRegularFile(fileSet.directory)) {
      throw new IOException("The [fileSet.directory] path [" + fileSet.directory + "] is a file and must be a directory");
    }

    if (!Files.isDirectory(fileSet.directory)) {
      throw new IOException("The [fileSet.directory] path [" + fileSet.directory + "] does not exist");
    }

    fileSets.add(fileSet);
    return this;
  }

  public ZipBuilder optionalFileSet(Path directory) throws IOException {
    return optionalFileSet(new FileSet(directory));
  }

  public ZipBuilder optionalFileSet(String directory) throws IOException {
    return optionalFileSet(Paths.get(directory));
  }

  public ZipBuilder optionalFileSet(FileSet fileSet) throws IOException {
    if (Files.isRegularFile(fileSet.directory)) {
      throw new IOException("The [fileSet.directory] path [" + fileSet.directory + "] is a file and must be a directory");
    }

    // Only add if it exists
    if (Files.isDirectory(fileSet.directory)) {
      fileSets.add(fileSet);
    }

    return this;
  }
}
