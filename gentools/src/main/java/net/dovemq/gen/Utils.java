package net.dovemq.gen;

class Utils
{
    private static final String TAB_SPACE = "    ";

    static String
    convertToCanonicalName(String input)
    {
        String[] inputs = input.split("-");
        StringBuilder s = new StringBuilder();
        for (String str : inputs)
        {
            char[] ar = str.toCharArray();
            ar[0] = Character.toUpperCase(ar[0]);            
            s.append(ar);
        }
        return s.toString();
    }
    
    static String
    lowerCapFirstLetter(String input)
    {
        char[] ar = input.toCharArray();
        ar[0] = Character.toLowerCase(ar[0]);
        return new String(ar);
    }
    
    static String
    upperCapFirstLetter(String input)
    {
        char[] ar = input.toCharArray();
        ar[0] = Character.toUpperCase(ar[0]);
        return new String(ar);
    }
    
    static String
    convertPackageNameToDir(String baseDir, String packageName)
    {
        String packageName2 = packageName.replace(".", "/");
        return String.format("%s/%s", baseDir, packageName2);
    }
    
    static String
    insertTabs(int numTabs, String input)
    {
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < numTabs; i++)
        {
            output.append(TAB_SPACE);
        }
        output.append(input);
        return output.toString();
    }    
}
