#
# Use Security TechSpecs from $CIP/configs/security/security rather than those in jar or cache
#

CIP = ENV['CIP']

insert_after :snapshot_restored do
  do_action "GenericAction" do |run|
    `sh #{CIP}/operator/updateSecurityTechspecs.sh`
  end
end

