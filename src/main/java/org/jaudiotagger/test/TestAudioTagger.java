/**
 * @author : Paul Taylor
 *
 * Version @version:$Id$
 *
 * Jaudiotagger Copyright (C)2004,2005
 *
 * This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public  License as published by the Free Software Foundation; either version 2.1 of the License,
 * or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this library; if not,
 * you can get a copy from http://www.opensource.org/licenses/lgpl-license.php or write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 *
 * Description:
 */
package org.jaudiotagger.test;

import org.jaudiotagger.audio.AudioFileFilter;
import org.jaudiotagger.audio.AudioFileIO;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;

/**
 * Simple class that will attempt to recusively read all files within a directory, flags
 * errors that occur.
 */
public class TestAudioTagger
{

    public static void main(final String[] args)
    {
        TestAudioTagger test = new TestAudioTagger();

        if (args.length == 0)
        {
            jmp123.Jmp123Logger.error("usage TestAudioTagger Dirname");
            jmp123.Jmp123Logger.error("      You must enter the root directory");
            System.exit(1);
        }
        else if (args.length > 1)
        {
            jmp123.Jmp123Logger.error("usage TestAudioTagger Dirname");
            jmp123.Jmp123Logger.error("      Only one parameter accepted");
            System.exit(1);
        }
        File rootDir = new File(args[0]);
        if (!rootDir.isDirectory())
        {
            jmp123.Jmp123Logger.error("usage TestAudioTagger Dirname");
            jmp123.Jmp123Logger.error("      Directory " + args[0] + " could not be found");
            System.exit(1);
        }
        Date start = new Date();
        jmp123.Jmp123Logger.info("Started to read from:" + rootDir.getPath() + " at " + DateFormat.getTimeInstance().format(start));
        test.scanSingleDir(rootDir);
        Date finish = new Date();
        jmp123.Jmp123Logger.info("Started to read from:" + rootDir.getPath() + " at " + DateFormat.getTimeInstance().format(start));
        jmp123.Jmp123Logger.info("Finished to read from:" + rootDir.getPath() + DateFormat.getTimeInstance().format(finish));
        jmp123.Jmp123Logger.info("Attempted  to read:" + count);
        jmp123.Jmp123Logger.info("Successful to read:" + (count - failed));
        jmp123.Jmp123Logger.info("Failed     to read:" + failed);

    }


    private static int count = 0;
    private static int failed = 0;

    /**
     * Recursive function to scan directory
     * @param dir
     */
    private void scanSingleDir(final File dir)
    {

        final File[] audioFiles = dir.listFiles(new AudioFileFilter(false));
        for (File audioFile : audioFiles) {
            count++;
            try {
                AudioFileIO.read(audioFile);
            } catch (Throwable t) {
                jmp123.Jmp123Logger.error("Unable to read record:" + count + ":" + audioFile.getPath());
                failed++;
                t.printStackTrace();
            }
        }

        final File[] audioFileDirs = dir.listFiles(new DirFilter());
        for (File audioFileDir : audioFileDirs) {
            scanSingleDir(audioFileDir);
        }
    }


    public final class DirFilter implements java.io.FileFilter
    {
        public DirFilter()
        {

        }


        /**
         * Determines whether or not the file is an mp3 file.  If the file is
         * a directory, whether or not is accepted depends upon the
         * allowDirectories flag passed to the constructor.
         *
         * @param file the file to test
         * @return true if this file or directory should be accepted
         */
        public boolean accept(final File file)
        {
            return file.isDirectory();
        }

        public static final String IDENT = "$Id$";
    }
}
