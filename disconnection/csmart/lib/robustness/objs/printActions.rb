=begin script

include_path: printActions.rb
description: Print most Generic Actions to console.

=end

insert_after :setup_run do
  do_action "GenericAction" do |run|
    run.comms.on_cougaar_event do |event|
      if !event.component.include?("Quiesc") &&
         !event.component.include?("ALDynamicSDClientPlugin")
        run.info_message event
      end
    end
  end
end
