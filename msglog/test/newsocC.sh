es
#!/bin/tcsh
rm SC-1AD-NEW-AL-STRIPPED.xml
rm SC-1AD-NEW-AL-STRIPPED.xml.rb
rm SC-1AD-NEW-AL-RULES.xml.rb
rm SC-1AD-NEW-AL-RULES-MSGLOG.xml.rb
cp newMASTER-SC-1AD-NEW-AL-STRIPPED.xml SC-1AD-NEW-AL-STRIPPED.xml
ruby edit.rb SC-1AD-NEW-AL-STRIPPED.xml
ruby ../../config/bin/transform_society.rb -i SC-1AD-NEW-AL-STRIPPED.xml.rb -r . -o SC-1AD-NEW-AL-RULES.xml.rb
ruby ../../config/bin/transform_society.rb -i SC-1AD-NEW-AL-RULES.xml.rb -r msglog-full-tic.rulex -o SC-1AD-NEW-AL-RULES-MSGLOG.xml.rb
