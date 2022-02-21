package work.liziyun.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public interface LzyJavaFile {
     // 修改时间: 判断文件是否改动
     long lastMod();
     // 简单类名: xxx.class
     String getSimpleName();
     // 文件名
     String getFileName();
     // 获取文件路径
     String getPath();
     // IO流
     InputStream open() throws IOException;
     // 获取文件类型
     Kind getKind();
     enum Kind {
          SOURCE(".java"),
          CLASS(".class"),
          HTML(".html"),
          OTHER("");
          public final String extension;
          private Kind(String extension) {
               this.extension = extension;
          }
     };


     /**
      * 压缩包文件
      */
     class JarFile implements LzyJavaFile {
          // 简单类名
          private String simpleName;
          // 文件名
          private String fileName;
          // 目录(压缩包)
          private ZipFile zipFile;
          // 文件(压缩包中文件)
          private ZipEntry zipEntry;
          // 类型
          private Kind kind;

          public JarFile(String fileName, ZipFile zipFile, ZipEntry zipEntry) {
               this.fileName = fileName;
               this.zipFile = zipFile;
               this.zipEntry = zipEntry;
               if (fileName.endsWith(".class")){
                    kind = Kind.CLASS;
                    this.simpleName = this.fileName.substring(0,this.fileName.length()-kind.extension.length());
               }else{
                    kind = Kind.SOURCE;
                    this.simpleName = this.fileName.substring(0,this.fileName.length()-kind.extension.length());
               }
          }

          @Override
          public long lastMod() {
               return zipEntry.getTime();
          }

          @Override
          public String getSimpleName() {
               return simpleName;
          }

          @Override
          public String getFileName() {
               return fileName;
          }


          @Override
          public String getPath() {
               // 压缩包(文件)
               return zipFile.getName() +"(" + zipEntry.getName() + ")";

          }

          @Override
          public InputStream open() throws IOException {
               return this.zipFile.getInputStream(this.zipEntry);
          }

          @Override
          public Kind getKind() {
               return kind;
          }
     }

     /**
      * 普通文件
      */
     class CommonFile implements LzyJavaFile {
          // 文件名称: xxx.class
          private String simpleName;
          //
          private String fileName;
          // 文件
          private File file;
          // 类型
          private Kind kind;

          public CommonFile(String fileName, File file) {
               this.fileName = fileName;
               this.file = file;
               if (fileName.endsWith(".class")){
                    kind = Kind.CLASS;
                    this.simpleName = this.fileName.substring(0,this.fileName.length()-kind.extension.length());
               }else{
                    kind = Kind.SOURCE;
                    this.simpleName = this.fileName.substring(0,this.fileName.length()-kind.extension.length());
               }
          }

          @Override
          public long lastMod() {
               return file.lastModified();
          }

          @Override
          public String getSimpleName() {
               return simpleName;
          }

          @Override
          public String getFileName() {
               return fileName;
          }



          @Override
          public String getPath() {
               return file.getPath();
          }

          @Override
          public InputStream open() throws IOException {
               return new FileInputStream(file);
          }

          @Override
          public Kind getKind() {
               return kind;
          }
     }

}

