package org.apache.maven.scm.provider.perforce.command;

import org.apache.maven.scm.log.ScmLogger;
import org.apache.maven.scm.provider.perforce.PerforceScmProvider;
import org.apache.maven.scm.provider.perforce.repository.PerforceScmProviderRepository;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.Commandline;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Encapsulates the 'p4 where' command which can be very useful in determining
 * a file's location within the depot.  Use <code>getDepotLocation(String path)</code> to query
 * the depot location for a particular file.  The data from p4 where looks like this:
 *
 * <pre>
   p4 where pom.xml
   //depot/modules/fabric/trunk/pom.xml //mikeperham-dt/depot/modules/fabric/trunk/pom.xml d:\perforce\depot\modules\fabric\trunk\pom.xml
   </pre>
 *
 * @author mperham
 */
public class PerforceWhereCommand
{
    private ScmLogger logger = null;
    private PerforceScmProviderRepository repo = null;

    public PerforceWhereCommand( ScmLogger log, PerforceScmProviderRepository repos )
    {
        logger = log;
        repo = repos;
    }

    public String getDepotLocation( File file )
    {
        return getDepotLocation( file.getAbsolutePath() );
    }

    /**
     * @param filepath an absolute file path
     * @return the absolute location of the given file within the Perforce repository or null if the file
     * does not exist in a mapping within the current clientspec.
     */
    public String getDepotLocation( String filepath )
    {
        if ( !PerforceScmProvider.isLive() )
        {
            return null;
        }

        try
        {
            Commandline command = PerforceScmProvider.createP4Command( repo, null );
            command.createArgument().setValue( "where" );
            command.createArgument().setValue( filepath );
            logger.debug( PerforceScmProvider.clean( "Executing: " + command.toString() ) );
            Process proc = command.execute();
            BufferedReader br = new BufferedReader( new InputStreamReader( proc.getInputStream() ) );
            String line;
            String path = null;
            while ( ( line = br.readLine() ) != null )
            {
                if ( line.indexOf( "not in client view" ) != -1 )
                {
                    // uh oh, something bad is happening
                    logger.error( line );
                    return null;
                }
                if ( line.indexOf( "is not under" ) != -1 )
                {
                    // uh oh, something bad is happening
                    logger.error( line );
                    return null;
                }

                logger.debug(line);
                // verify that "//" appears twice in the line
                path = line.substring( 0, line.lastIndexOf( "//" ) - 1 );
            }
            return path;
        }
        catch ( CommandLineException e )
        {
            logger.error( e );
            throw new RuntimeException( e.getLocalizedMessage() );
        }
        catch ( IOException e )
        {
            logger.error( e );
            throw new RuntimeException( e.getLocalizedMessage() );
        }
    }
}