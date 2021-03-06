<?php if (!defined('BASEPATH')) {
    exit('No direct script access allowed');
}

class Register extends CI_Controller
{
    public function __construct()
    {
        parent::__construct();
        $this->load->model('user', '', true);
        $this->load->library('form_validation');
        $this->form_validation->set_error_delimiters('<span class="warning">', '</span>');

        $this->load->library('email');
        $config['protocol'] = 'sendmail';
        $config['mailtype'] = 'html';
        $this->email->initialize($config);

        $this->done = false;
    }

    public function index()
    {
        $this->load->helper(array('form'));
        $this->load->view('register');
    }

    public function registerUser()
    {
        $this->form_validation->set_rules('firstName', 'First Name', 'required');
        $this->form_validation->set_rules('lastName', 'Last Name', 'required');
        $this->form_validation->set_rules('email', 'Email', 'required|valid_email');

        if ($this->form_validation->run() == false) {
            $this->load->view('register');
        } else {
            // Form is filled out properly
            // Does the email already exist?
            if ($this->user->isDuplicate($this->input->post('email'))) {
                $this->session->set_flashdata('flash_message', 'User email already exists.');
                redirect(site_url() . '/register');
            } else {
                // Insert user into tempUser table
                // Returns all post items with xss filter
                // xss_clean is intended to prevent a hacker from using Cross Site Scripting
                // attacks to hijack cookies or do other malicious things.
                $clean = $this->security->xss_clean($this->input->post(null, true));
                $id = $this->user->insertUser($clean);
                $token = $this->user->insertToken($id);

                $qstring = base64_encode($token);
                $url = site_url() . '/complete/completeReg/token/' . $qstring;
                $link = '<a href="' . $url . '">' . $url . '</a>';

                $message = 'Welcome to UAA NLP, the Semantic Network Analysis Pipeline<br/>';
                $message .= '<strong>Click the link below to finish registration</strong><br/>';
                $message .= $link;	

                // NOTE: the domain name here must match one of the FQDNs in the machine's hosts file
                $this->email->from('register@up.edu', 'U of Portland NLP - SNAP registration');
                $this->email->to($this->input->post('email'));

                $this->email->subject('U of Portland NLP - Registration');
                $this->email->message($message);

                $this->email->send();

                $this->done = true;
                $this->load->view('register');
            }
        }
    }
}
