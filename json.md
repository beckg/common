<b>A JSON tool</b>
    
I use a modified version of JSON to store configuration and state. I find that it is clear, lightweight, and 
easy to work with.
<br><br>
The material differences are:
<ul>
  <li>
    The use of commas is option - when you investigate the syntax you find that they are always implied. 
    Therefore codings like <b>[A B C]</b> - are legal.
  </li>
  <li>
    Outside of quotes the java comment "//" is recognised. Text up to the next new-line is ignored. So things like
    <b>A : B // default is B\n C:D</b> - are legal. Some people will hate this!
  </li>
  <li>
    Strings do not need to be quoted unless they contain semmantics ([ ] { } " : , or whitespace)
  </li>
  <li>
    Values are freely convertible between their native type and String. Therefore getInteger() and getString() are
    both successful provided that the value is a naked or quoted integer - 3 or "3". Therefore add("a",1) and add("a","1") have identical results.
  </li>
</ul>
If a JSONObject is externalised (toString()) then the String will always be in standard JSON format. However, I often 
tweak the saved format - the strip layout files are modified to be easily readable. The exception is that values that 
look like numbers will be treated like numbers even if added as Strings.
<br><br>
You will need to look in the code for details of the APIs - but they really are obvious. You can also look in the other 
modules for examples of use.
<br><br>
<pre>
JSONObject object1 = new JSONObject("{a:b c:[1,2 3]}"); // see note1 

JSONObject object2 = new JSONObject();
object2.add("x", object1);  // see note 2
object2.add("y", "ab\"'\t yz");

JSONArray array1 = new JSONArray();
array1.add(2);
array1.add("cc");
object2.add("z", array1);

String a = object1.getString("a");
a = object2.getObject("x").getString("a"); // see note 3
JSONMap map = object2.getMap();
JSONList list = object2.getList(); // see note 4
JSONArray array = object2.getArray("z");
int c0 = object2.getObject("x").getArray("c").getInteger(0);
</pre>
Note 1: Notice that I can be perverse and sometimes use commas and sometimes not. 
<br>
Note 2: A <i>copy</i> of the source object is added - any subsequent amendments to object1 do not affect object2. 
<br>
Note 3: A <i>copy</i> of the source is fetched. In particular object2.getObject("x").add("f",1) achieves nothing.
<br>
Note 4: There is no validation that a specific key is not added twice. Both values will be in the object. A JSONMap
will only contain the <i>first</i> occurance, a JSONList will contain both.
<br>
Note 5: You cannot get a JSONObject or a JSONArray as a String. object1.getString("c") will fail;
object1.getArray("c").toString() will work.
