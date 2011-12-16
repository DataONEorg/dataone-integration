#!/usr/bin/perl

# this script is used to stub out the test methods for either the 
# CNode methods or the MNode methods. 
# the $sourceFile value can be a relative path or absolute.
#
# 

# Get list of methods from interface defs
$sourceFile = "/Users/rnahf/software/workspace/d1_libclient_java/src/main/java/org/dataone/client/CNode.java";
open(my $fh,"<",$sourceFile) or die "Can't open file: $!";
while (<$fh>) {
    next unless /^\s+public\s+(\w+)\s+(\w+)/;
    $method = $2;
    $type = $1;
    $methodList{$method} = $type;
    print STDERR "   $method  ($type)\n";
    push(@methodList,$method);
}

print STDERR "Methods found: " . scalar(@methodList) . "\n";

# create test template for each method

@template = <DATA>;
$template = join('',@template);

#print scalar(@methodList), " methods templated\n\n";

foreach $m (@methodList) {
    $type = $methodList{$m};
    $ucMethod = ucfirst($m);
    $t = $template;
    $t=~s/{Method}/$ucMethod/g;
    $t=~s/{method}/$m/g;
    $t=~s/{TYPE}/$type/g;
    print $t;
}

__END__


    @Test
    public void test{Method}() {
	Iterator<Node> it = getCoordinatingNodeIterator();
	while (it.hasNext()) {
	    currentUrl = it.next().getBaseURL();
	    CNode cn = new CNode(currentUrl);
	    printTestHeader("test{Method}(...) vs. node: " + currentUrl);
	    
	    try {
		ObjectInfo oi = getPrefetchedObject(currentUrl,0);    
		log.debug("   pid = " + oi.getIdentifier());

		{TYPE} response = cn.{method}();
		checkTrue(currentUrl,"{method}(...) returns a {TYPE} object", response != null);
		// checkTrue(currentUrl,"response cannot be false. [Only true or exception].", response);
	    } 
	    catch (IndexOutOfBoundsException e) {
		handleFail(currentUrl,"No Objects available to test against");
	    }
	    catch (BaseException e) {
		handleFail(currentUrl,e.getDescription());
	    }
	    catch(Exception e) {
		e.printStackTrace();
		handleFail(currentUrl,e.getClass().getName() + ": " + e.getMessage());
	    }
	}
    }
