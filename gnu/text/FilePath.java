// Copyright (c) 2007  Per M.A. Bothner.
// This is free software; for terms and warranty disclaimer see ../../COPYING.

package gnu.text;
import java.io.*;
import java.net.*;
import gnu.mapping.*;

/** A wrapper around a {@code java.io.File} that extends {@code Path}. */

public class FilePath
  extends Path
  /* #ifdef JAVA2 */
  /* #ifdef JAVA5 */
  implements Comparable<FilePath>
  /* #else */
  // implements Comparable
  /* #endif */
  /* #endif */
{
  final File file;
  /** Usually the same as {@code file.toString()}.
   * One important difference: {@code isDirectory} is true
   * if {@code path} ends with the {@code '/'} or the {@code separatorChar}.
   * The original String if constructed from a String.
   */
  final String path;

  private FilePath (File file)
  {
    this.file = file;
    this.path = file.toString();
  }

  private FilePath (File file, String path)
  {
    this.file = file;
    this.path = path;
  }

  public static FilePath valueOf (String str)
  {
    String orig = str;
    /* FIXME: Should we expand '~'?
       Issues: is (path "~/bar") absolute?
       What about: (base:resolve "~/bar") ?
       What if base above isn't a FilePath?
    int len = str.length();
    if (len > 0 && str.charAt(0) == '~' && File.separatorChar == '/')
      {
        if (len == 1 || str.charAt(1) == '/')
          {
            String user = System.getProperty("user.home");
            if (user != null)
              str = user + str.substring(1);
          }
        else
          {
            // We don't support '~USER/...'  Do that using /bin/sh. FIXME
          }
      }
    */
    return new FilePath(new File(str), orig);
  }

  public static FilePath valueOf (File file)
  {
    return new FilePath(file);
  }

  public static FilePath coerceToFilePathOrNull (Object path)
  {
    if (path instanceof FilePath)
      return (FilePath) path;
    if (path instanceof URIPath)
      return FilePath.valueOf(new File(((URIPath) path).uri));
    /*
    if (path instanceof URL)
      return URLPath.valueOf((URL) path);
    */
    /* #ifdef use:java.net.URI */
    if (path instanceof URI)
      return FilePath.valueOf(new File((URI) path));
    /* #endif */
    if (path instanceof File)
      return FilePath.valueOf((File) path);
    String str;
    if (path instanceof gnu.lists.FString) // FIXME: || UntypedAtomic
      str = path.toString();
    else if (path instanceof String)
      str = (String) path;
    else
      return null;
    return FilePath.valueOf(str);
  }
  public static FilePath makeFilePath (Object arg)
  {
    FilePath path = coerceToFilePathOrNull(arg);
    if (path == null)
      throw new WrongType((String) null, WrongType.ARG_CAST, arg, "filepath");
    return path;
  }

  public boolean isAbsolute ()
  {
    return this == Path.userDirPath || file.isAbsolute();
  }

  public boolean isDirectory ()
  {
    if (file.isDirectory())
      return true;
    if (! file.exists())
      {
        int len = path.length();
        if (len > 0)
          {
            char last = path.charAt(len - 1);
            if (last == '/' || last == File.separatorChar)
              return true;
          }
      }
    return false;
  }

  public boolean delete ()
  {
    return toFile().delete();
  }

  public long getLastModified ()
  {
    return file.lastModified();
  }

  public boolean exists ()
  {
    return file.exists();
  }

  public long getContentLength ()
  {
    long length = file.length();
    return length == 0 && ! file.exists() ? -1 : length;
  }
 
  public String getPath ()
  {
    return file.getPath();
  }

  public String getLast ()
  {
    return file.getName();
  }

  public
  /* #ifdef JAVA5 */
  FilePath
  /* #else */
  // Path
  /* #endif */
  getParent ()
  {
    File parent = file.getParentFile();
    if (parent == null)
      return null;
    else
      return FilePath.valueOf(parent);
  }

  public int compareTo (FilePath path)
  {
    return file.compareTo(path.file);
  }

  /* #ifndef JAVA5 */
  // public int compareTo (Object obj)
  // {
  //   return compareTo((FilePath) obj);
  // }
  /* #endif */

  public boolean equals (Object obj)
  {
    return obj instanceof FilePath && file.equals(((FilePath) obj).file);
  }

  public int hashCode ()
  {
    return file.hashCode();
  }

  public String toString ()
  {
    return path;
  }

  public File toFile ()
  {
    return file;
  }

  public URL toURL ()
  {
    if (this == Path.userDirPath)
      return resolve("").toURL();
    if (! isAbsolute())
      return getAbsolute().toURL();
    try
      {
        /* #ifdef JAVA2 */
        /* #ifdef use:java.net.URI */
        return file.toURI().toURL();
        /* #else */
        // return file.toURL();
        /* #endif */
        /* #else */
        // char fileSep = File.separatorChar;
        // return new URL("file:" + file.getAbsolutePath().replace(fileSep, '/'));
        /* #endif */
      }
    catch (Throwable ex)
      {
        throw WrappedException.wrapIfNeeded(ex);
      }
  }

  /* #ifndef use:java.net.URI */
  // public String toURIString ()
  // {
  //   if (file.isAbsolute())
  //     return toURL().toString();
  //   else
  //     return file.toString().replace(File.separatorChar, '/');
  // }
  /* #endif */

  /* #ifdef use:java.net.URI */
  private static URI toUri (File file)
  {
    try
      {
        if (file.isAbsolute())
          return file.toURI();
        /* We don't want to just use File.toURI(),
           because that turns a relative File into an absolute URI. */
        String fname = file.toString();
        char fileSep = File.separatorChar;
        if (fileSep != '/')
          fname = fname.replace(fileSep, '/');
        return new URI(null, null, fname, null);
      }
    catch (Throwable ex)
      {
        throw WrappedException.wrapIfNeeded(ex);
      }

  }

  public URI toUri ()
  {
    if (this == Path.userDirPath)
      return resolve("").toURI();
    return toUri(file);
  }
  /* #endif */

  public InputStream openInputStream () throws IOException
  {
    return new FileInputStream(file);
  }

  public OutputStream openOutputStream () throws IOException
  {
    return new FileOutputStream(file);
  }

  public String getScheme ()
  {
    return isAbsolute() ? "file" : null;
  }

  public Path resolve (String relative)
  {
    if (Path.uriSchemeSpecified(relative))
      return URLPath.valueOf(relative);
    File rfile = new File(relative);
    if (rfile.isAbsolute())
      return FilePath.valueOf(rfile);
    char sep = File.separatorChar;
    if (sep != '/')
      relative = relative.replace('/', sep);
    // FIXME? Or: if (file.getPath().length() == 0) return relative;
    File nfile;
    if (this == Path.userDirPath)
      nfile = new File(System.getProperty("user.dir"), relative);
    else
      nfile = new File(isDirectory() ? file : file.getParentFile(), relative);
    return valueOf(nfile);
  }

  public Path getCanonical ()
  {
    try
      {
        File canon = file.getCanonicalFile();
        if (! canon.equals(file))
          return valueOf(canon);
      }
    catch (Throwable ex)
      {
      }
    return this;
  }
}
